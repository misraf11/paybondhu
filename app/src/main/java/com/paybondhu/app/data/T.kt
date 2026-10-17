package com.paybondhu.app.data

/** All user-visible text, English and Bangla. Reads [Lang] so Compose recomposes on change. */
object T {
    private fun p(en: String, bn: String): String = if (Lang.bn) bn else en

    // Tabs
    val tabHome get() = p("Home", "হোম")
    val tabHistory get() = p("History", "হিস্টোরি")
    val tabSettings get() = p("Settings", "সেটিংস")

    // Login
    val loginTitle get() = p("Connect this phone", "এই ফোন কানেক্ট করুন")
    val loginSub get() = p(
        "In your PayBondhu dashboard open Integration, then copy your email and device key.",
        "PayBondhu ড্যাশবোর্ডের Integration পেজ থেকে আপনার ইমেইল ও ডিভাইস কী কপি করুন।"
    )
    val email get() = p("Account email", "অ্যাকাউন্ট ইমেইল")
    val deviceKey get() = p("Device key", "ডিভাইস কী")
    val paste get() = p("Paste", "পেস্ট")
    val connect get() = p("Connect", "কানেক্ট করুন")
    val connecting get() = p("Connecting…", "কানেক্ট হচ্ছে…")
    val advanced get() = p("Server settings", "সার্ভার সেটিংস")
    val serverUrl get() = p("Server address", "সার্ভারের ঠিকানা")
    val errEmpty get() = p("Enter your email and device key.", "ইমেইল ও ডিভাইস কী দিন।")
    val errNetwork get() = p("Can't reach the server. Check your internet connection.", "সার্ভারে পৌঁছানো যাচ্ছে না। ইন্টারনেট চেক করুন।")
    val errInvalid get() = p("Email or device key is wrong.", "ইমেইল বা ডিভাইস কী ভুল।")
    val errExpired get() = p(
        "Your plan has expired or the device limit is reached. Check your plan in the dashboard.",
        "প্ল্যানের মেয়াদ শেষ বা ডিভাইস লিমিট পূর্ণ। ড্যাশবোর্ডে প্ল্যান চেক করুন।"
    )
    val errOther get() = p(
        "This key is already connected to another phone. Disconnect it there or change the key in the dashboard.",
        "এই কী অন্য ফোনে কানেক্ট করা আছে। সেখানে ডিসকানেক্ট করুন অথবা ড্যাশবোর্ডে কী বদলান।"
    )
    val errServer get() = p("The server sent an unexpected reply. Try again in a moment.", "সার্ভার থেকে অপ্রত্যাশিত উত্তর এসেছে। একটু পরে আবার চেষ্টা করুন।")

    // Status
    val statusActive get() = p("Sync active", "সিঙ্ক চালু আছে")
    val statusSyncing get() = p("Syncing…", "সিঙ্ক হচ্ছে…")
    val statusOffline get() = p("Waiting for internet", "ইন্টারনেটের অপেক্ষায়")
    val statusAction get() = p("Action needed", "পদক্ষেপ দরকার")
    val offlineHint get() = p("Messages are saved on this phone and upload as soon as you are online.", "মেসেজ এই ফোনে সেভ আছে, ইন্টারনেট এলেই আপলোড হবে।")
    val neverSynced get() = p("Not synced yet", "এখনো সিঙ্ক হয়নি")
    fun lastSynced(x: String) = p("Last synced $x", "সর্বশেষ সিঙ্ক: $x")
    val syncNow get() = p("Sync now", "এখনই সিঙ্ক")
    val justNow get() = p("just now", "এইমাত্র")
    fun minAgo(n: Int) = p("$n min ago", "$n মিনিট আগে")
    fun hourAgo(n: Int) = p("$n h ago", "$n ঘণ্টা আগে")
    fun dayAgo(n: Int) = p("$n d ago", "$n দিন আগে")

    // Home
    val statToday get() = p("Today", "আজ")
    val statPending get() = p("Pending", "অপেক্ষমাণ")
    val statSent get() = p("Sent", "পাঠানো")
    val receivedToday get() = p("Received today", "আজকের প্রাপ্তি")
    val recent get() = p("Recent transactions", "সাম্প্রতিক লেনদেন")
    val viewAll get() = p("View all", "সব দেখুন")
    val emptyTitle get() = p("No transactions yet", "এখনো কোনো লেনদেন নেই")
    val emptySub get() = p(
        "Payment SMS from bKash, Nagad, Rocket and others will appear here automatically.",
        "বিকাশ, নগদ, রকেট ইত্যাদির পেমেন্ট SMS এখানে নিজে থেকেই দেখা যাবে।"
    )
    val authTitle get() = p("This phone is not authorised", "এই ফোনের অনুমতি নেই")
    val authExpired get() = p("Your plan has expired or the device limit is reached.", "প্ল্যানের মেয়াদ শেষ বা ডিভাইস লিমিট পূর্ণ।")
    val authOther get() = p("This key is connected to another phone.", "এই কী অন্য ফোনে কানেক্ট করা আছে।")
    val authInvalid get() = p("The device was removed or its key changed. Connect again.", "ডিভাইসটি মুছে ফেলা হয়েছে বা কী বদলানো হয়েছে। আবার কানেক্ট করুন।")
    val reconnect get() = p("Reconnect", "আবার কানেক্ট")
    val updateTitle get() = p("Update available", "নতুন ভার্সন এসেছে")
    fun updateSub(v: String) = p("Version $v is ready to install.", "ভার্সন $v ইনস্টলের জন্য প্রস্তুত।")
    val download get() = p("Download", "ডাউনলোড")
    val setupTitle get() = p("Finish setup", "সেটআপ শেষ করুন")
    val setupSub get() = p("Allow these so payments are never missed.", "পেমেন্ট মিস না হতে এগুলো চালু করুন।")
    val permSms get() = p("SMS access", "SMS এক্সেস")
    val permSmsSub get() = p("Read payment messages", "পেমেন্ট মেসেজ পড়ার জন্য")
    val permNotif get() = p("Notifications", "নোটিফিকেশন")
    val permNotifSub get() = p("Show the sync status", "সিঙ্ক স্ট্যাটাস দেখানোর জন্য")
    val permBattery get() = p("Run in background", "ব্যাকগ্রাউন্ডে চালু")
    val permBatterySub get() = p("Don't let Android pause the app", "অ্যান্ড্রয়েড যেন অ্যাপ বন্ধ না করে")
    val allow get() = p("Allow", "অনুমতি")
    val allowed get() = p("Allowed", "দেওয়া আছে")

    // History
    val searchHint get() = p("Search TrxID, number or text", "TrxID, নম্বর বা লেখা খুঁজুন")
    val fAll get() = p("All", "সব")
    val fPending get() = p("Pending", "অপেক্ষমাণ")
    val fSent get() = p("Sent", "পাঠানো")
    val fMatched get() = p("Matched", "ম্যাচ")
    val fFailed get() = p("Failed", "ব্যর্থ")
    val pillPending get() = p("Pending", "অপেক্ষমাণ")
    val pillSent get() = p("Sent", "পাঠানো হয়েছে")
    val pillMatched get() = p("Matched", "ম্যাচ হয়েছে")
    val pillFailed get() = p("Failed", "ব্যর্থ")
    val pillSaved get() = p("Saved", "সংরক্ষিত")
    val today get() = p("Today", "আজ")
    val yesterday get() = p("Yesterday", "গতকাল")
    val noResults get() = p("Nothing found", "কিছু পাওয়া যায়নি")
    val loadMore get() = p("Load more", "আরও দেখুন")
    val detailTitle get() = p("Transaction", "লেনদেন")
    val dAmount get() = p("Amount", "পরিমাণ")
    val dFrom get() = p("From", "প্রেরক")
    val dTrx get() = p("TrxID", "TrxID")
    val dTime get() = p("Received", "সময়")
    val dStatus get() = p("Status", "স্ট্যাটাস")
    val dMessage get() = p("Message", "মেসেজ")
    val copy get() = p("Copy", "কপি")
    val copied get() = p("Copied", "কপি হয়েছে")
    val retry get() = p("Send again", "আবার পাঠান")
    val explainPending get() = p("Waiting to upload.", "আপলোডের অপেক্ষায়।")
    val explainSent get() = p("Delivered to PayBondhu. Waiting for a customer payment to use it.", "PayBondhu-তে পৌঁছেছে। গ্রাহকের পেমেন্ট ম্যাচের অপেক্ষায়।")
    val explainMatched get() = p("Used to confirm a payment.", "একটি পেমেন্ট কনফার্ম করতে ব্যবহৃত হয়েছে।")
    val explainSaved get() = p("Older message kept for your records only.", "পুরনো মেসেজ, শুধু রেকর্ডের জন্য রাখা।")
    val explainFailedPrefix get() = p("Not accepted", "গ্রহণ করা হয়নি")

    // Settings
    val account get() = p("Account", "অ্যাকাউন্ট")
    val device get() = p("Device", "ডিভাইস")
    val server get() = p("Server", "সার্ভার")
    val planUntil get() = p("Plan valid until", "প্ল্যানের মেয়াদ")
    val syncSection get() = p("Sync", "সিঙ্ক")
    val autoScan get() = p("Check inbox for missed SMS", "মিস হওয়া SMS ইনবক্স থেকে খুঁজুন")
    val autoScanSub get() = p("Only payment messages from bKash, Nagad, Rocket etc. are used. Nothing else leaves the phone.", "শুধু বিকাশ, নগদ, রকেট ইত্যাদির পেমেন্ট মেসেজ ব্যবহার হয়। অন্য কিছু ফোন থেকে বের হয় না।")
    val importNow get() = p("Import last 30 days", "গত ৩০ দিনের লেনদেন আনুন")
    fun imported(n: Int) = p("$n new messages found", "$n টি নতুন মেসেজ পাওয়া গেছে")
    val needSmsRead get() = p("Allow SMS access first", "আগে SMS এক্সেস দিন")
    val keepAlive get() = p("Keep it running", "সবসময় চালু রাখুন")
    val keepAliveTip get() = p(
        "On Xiaomi, Oppo, Vivo, Realme, Samsung and similar phones also allow Auto-start, set Battery to “No restrictions”, and lock the app in Recent apps.",
        "Xiaomi, Oppo, Vivo, Realme, Samsung ইত্যাদি ফোনে Auto-start চালু করুন, Battery “No restrictions” দিন এবং Recent apps-এ অ্যাপটি লক করে রাখুন।"
    )
    val openSettings get() = p("Open app settings", "অ্যাপ সেটিংস খুলুন")
    val language get() = p("Language", "ভাষা")
    val about get() = p("About", "অ্যাপ সম্পর্কে")
    val version get() = p("Version", "ভার্সন")
    val checkUpdate get() = p("Check for update", "আপডেট খুঁজুন")
    val upToDate get() = p("You have the latest version", "আপনার কাছে সর্বশেষ ভার্সন আছে")
    val disconnect get() = p("Disconnect this phone", "এই ফোন ডিসকানেক্ট করুন")
    val disconnectAsk get() = p("Stop syncing and sign out?", "সিঙ্ক বন্ধ করে সাইন আউট করবেন?")
    val disconnectSub get() = p("Saved history stays on this phone. You can connect again any time.", "সংরক্ষিত হিস্টোরি এই ফোনেই থাকবে। যেকোনো সময় আবার কানেক্ট করতে পারবেন।")
    val cancel get() = p("Cancel", "বাতিল")
    val yes get() = p("Disconnect", "ডিসকানেক্ট")

    // Notification
    val notifActive get() = p("PayBondhu · Sync active", "PayBondhu · সিঙ্ক চালু আছে")
    val notifOffline get() = p("PayBondhu · Waiting for internet", "PayBondhu · ইন্টারনেটের অপেক্ষায়")
    val notifAction get() = p("PayBondhu · Action needed", "PayBondhu · পদক্ষেপ দরকার")
    fun notifLine(last: String, pending: Int, today: Int) =
        p("Last sync $last · $pending pending · $today today", "সর্বশেষ সিঙ্ক $last · অপেক্ষমাণ $pending · আজ $today")
    val notifOpen get() = p("Open", "খুলুন")
    val channelName get() = p("Sync status", "সিঙ্ক স্ট্যাটাস")
    val channelDesc get() = p("Shows that PayBondhu is running and syncing payments.", "PayBondhu চালু আছে ও পেমেন্ট সিঙ্ক করছে তা দেখায়।")
}
