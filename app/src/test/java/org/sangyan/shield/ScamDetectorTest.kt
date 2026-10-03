package org.sangyan.shield
import org.junit.Assert.*
import org.junit.Test
import org.sangyan.shield.domain.detector.*
import org.sangyan.shield.privacy.SensitiveDataMasker
import com.google.gson.Gson
class ScamDetectorTest {
 private val detector=ScamDetector(DetectorConfig { javaClass.classLoader!!.getResource("config/$it")!!.readText() })
 private fun high(text:String) { val result=detector.analyze(text); assertTrue("Expected high risk, got ${result.score}: $text",result.score>=50) }
 private fun low(text:String) { val result=detector.analyze(text); assertTrue("Expected low risk, got ${result.score}: $text",result.score<25) }
 @Test fun urgentScam()=high("Your account will be suspended within 2 hours. Verify immediately")
 @Test fun otpTheft()=high("Send your OTP to verify your account")
 @Test fun protectiveOtp()=low("Your OTP is 482921. Do not share this OTP with anyone. Bank employees will never ask for it.")
 @Test fun guaranteedReturn()=high("Guaranteed 30% monthly profit. Join Telegram now.")
 @Test fun nsdlImpersonation() { val r=detector.analyze("NSDL: Complete KYC immediately at https://nsdl-secure-kyc.xyz"); assertTrue(r.score>=75); assertTrue(r.findings.any { it.category=="Possible NSDL impersonation" }) }
 @Test fun sebiImpersonation()=high("SEBI registered expert. Guaranteed return. https://sebi-advice.xyz")
 @Test fun suspiciousUrl() { val r=detector.analyze("http://sebi.gov.in@192.168.1.1/verify?otp=123456"); assertTrue(r.components.getValue("url")>=70); assertTrue(r.score>=25) }
 @Test fun normalUrl()=low("Read the investor notice at https://www.sebi.gov.in")
 @Test fun remoteAccess()=high("Install AnyDesk so our support executive can fix your trading account.")
 @Test fun processingFee()=high("Pay ₹4,999 processing fee to release ₹2,50,000.")
 @Test fun hindiScam()=high("आपका खाता बंद हो जाएगा। तुरंत KYC अपडेट करें। OTP शेयर करें।")
 @Test fun hinglishScam()=high("Aapka demat account aaj block ho jayega. KYC turant update kare: http://secure-nsdl-kyc.xyz")
 @Test fun normalBankWarning()=low("Bank staff will never ask you to share OTP. Do not share PIN or password.")
 @Test fun normalTransaction()=low("Your account was credited with INR 5,000. Available balance INR 12,000.")
 @Test fun protectiveClauseDoesNotExcuseScam()=high("Never share your OTP. Send your OTP to verify your account.")
 @Test fun laterWarningDoesNotEraseRequest()=high("Send OTP now, never delay")
 @Test fun shortLinkAloneIsNotFraud()=low("https://bit.ly/abc")
 @Test fun hindiToggle() { val a=detector.analyze("पैसा डबल",false); assertEquals(0,a.score) }
 @Test fun domainSuffixAttack() { val r=detector.analyze("NSDL https://nsdl.co.in.evil.xyz"); assertTrue(r.findings.any { it.category=="Possible NSDL impersonation" }) }
 @Test fun typoDomain() { val r=detector.analyze("https://zerodna.com"); assertTrue(r.findings.any { it.category=="Possible Zerodha impersonation" }) }
 @Test fun noSensitivePersistence() {
  val raw="Send OTP 482921 password AbcSecret! and PIN 1234 at https://fake.xyz?password=secretValue"
  val saved=Gson().toJson(SensitiveDataMasker.forStorage(detector.analyze(raw)))
  listOf("482921","AbcSecret","1234","secretValue","fake.xyz").forEach { assertFalse("Leaked $it",saved.contains(it)) }
 }
 @Test fun fullWidthOtpMasking() { assertFalse(SensitiveDataMasker.mask("OTP １２３４５６").contains("１２３４５６")) }
 @Test fun boundedScores() { listOf("", "normal", "URGENT ".repeat(5000),"Send OTP. Guaranteed profit. Install AnyDesk. https://nsdl-kyc.xyz").forEach { assertTrue(detector.analyze(it).score in 0..100) } }
 @Test fun misleadingNegation()=high("Never hesitate to send OTP now")
 @Test fun sameClauseRequest()=high("Never share OTP, send OTP to verify your account")
 @Test fun warningAgainstReturns()=low("Beware of guaranteed returns. Never install AnyDesk.")
 @Test fun caseInsensitive()=high("SEND YOUR OTP NOW")
 @Test fun legitimateSubdomain()=low("SEBI https://investor.sebi.gov.in/info")
 @Test fun safeWarningWithLink()=low("NSDL: Never share your OTP. Visit https://nsdl.co.in")
}
