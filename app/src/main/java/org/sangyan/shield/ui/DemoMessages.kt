package org.sangyan.shield.ui
data class DemoMessage(val title:String,val expected:String,val text:String)
val demoMessages=listOf(
 DemoMessage("Demat suspension","High / severe","Dear customer, your NSDL Demat account will be suspended within 2 hours. Complete KYC immediately at https://nsdl-secure-kyc.xyz"),
 DemoMessage("Guaranteed profits","High / severe","SEBI registered expert. Guaranteed 30% monthly profit. Join Telegram now."),
 DemoMessage("Withdrawal fee","High / severe","Your investment withdrawal is ready. Pay ₹4,999 processing fee to release ₹2,50,000."),
 DemoMessage("Remote access","High / severe","Install AnyDesk so our support executive can fix your trading account."),
 DemoMessage("Protective OTP notice","Low","Your OTP is 482921. Do not share this OTP with anyone. Bank employees will never ask for it."),
 DemoMessage("Hinglish KYC threat","High / severe","Aapka demat account aaj block ho jayega. KYC turant update kare: http://secure-nsdl-kyc.xyz"),
 DemoMessage("Hindi credential theft","High / severe","आपका खाता बंद हो जाएगा। तुरंत KYC अपडेट करें। OTP शेयर करें।")
)
