import javax.mail.internet.InternetAddress

recipients = [new InternetAddress(build.getEnvVars()["ghprbActualCommitAuthorEmail"])]
msg.setRecipients(javax.mail.Message.RecipientType.TO, recipients as javax.mail.Address[])
