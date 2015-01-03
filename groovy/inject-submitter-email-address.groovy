import javax.mail.internet.InternetAddress

opt_out = build.getEnvVars().get("EMAIL_OPT_OUT", "").split(/(;|,|\n)/)

recipients = [new InternetAddress(build.getEnvVars()["ghprbActualCommitAuthorEmail"])]
// Filter opt-out addresses
opt_out_filtered = recipients.findAll { addr -> opt_out.contains(addr.toString()) }
msg.setRecipients(javax.mail.Message.RecipientType.TO, opt_out_filtered as javax.mail.Address[])
