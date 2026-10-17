"""Minimal stand-in for the PayBondhu API, used only by the CI emulator test."""
import json
import urllib.parse
from http.server import BaseHTTPRequestHandler, HTTPServer

LOG = open("received.log", "a", buffering=1)
USED_MARK = "NGD91ZQ4ML"  # the second test SMS is reported back as "used" by the server


class H(BaseHTTPRequestHandler):
    def log_message(self, *a):
        pass

    def _send(self, obj, code=200):
        data = json.dumps(obj).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_POST(self):
        n = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(n).decode()
        p = {k: v[0] for k, v in urllib.parse.parse_qs(body).items()}
        LOG.write(json.dumps({"path": self.path, "params": p}, ensure_ascii=False) + "\n")
        if self.path.endswith("/api/device-connect"):
            return self._send({"status": "1", "code": "ok", "message": "7", "device_name": "Shop phone"})
        if self.path.endswith("/api/device-status"):
            out = {
                "status": 1, "code": "ok", "message": "ok", "device_name": "Shop phone",
                "server_time": "2026-10-17 14:40:00", "plan_expire": "2027-01-01 00:00:00",
                "queue": {"waiting": 1, "used": 1, "today": 2, "last_at": "2026-10-17 14:36:00"},
                "app": {"latest_code": 20, "latest_name": "2.0.0", "min_code": 20, "apk_url": "http://10.0.2.2:8080/app.apk"},
            }
            if p.get("recent") == "1":
                out["recent"] = [{
                    "id": 2, "address": "nagad", "used": 1, "created_at": "2026-10-17 14:36:00",
                    "message": "Money Received. Amount: Tk 500.00 Sender: 01898765432 Ref: N/A TxnID: NGD91ZQ4ML Balance: Tk 2,100.00 17/10/2026 14:35",
                }]
            return self._send(out)
        if self.path.endswith("/api/add-data"):
            if "REJECTME" in p.get("message", ""):
                return self._send({"status": "0", "code": "invalid", "message": "SMS does not contain complete payment data"})
            return self._send({"status": 1, "code": "ok", "message": "Data inserted successfully"})
        if self.path.endswith("/api/device-disconnect"):
            return self._send({"status": 1, "code": "ok", "message": "Disconnected"})
        return self._send({"status": 0, "message": "not found"}, 404)


HTTPServer(("0.0.0.0", 8080), H).serve_forever()
