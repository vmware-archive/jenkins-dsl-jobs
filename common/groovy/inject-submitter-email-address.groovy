import javax.mail.internet.InternetAddress

opt_out = build.getEnvironment().get("EMAIL_OPT_OUT", "").split(/(;|,|\n)/)
actual_email_address = build.getEnvironment().get("ghprbActualCommitAuthorEmail", null)
if ( actual_email_address != null ) {
    recipients = [new InternetAddress(actual_email_address)]
    // Filter opt-out addresses
    opt_out_filtered = recipients.findAll { addr -> opt_out.contains(addr.toString()) == false }
    msg.setRecipients(javax.mail.Message.RecipientType.TO, opt_out_filtered as javax.mail.Address[])
}
