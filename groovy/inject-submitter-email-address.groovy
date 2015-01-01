@GrabResolver(name='jenkins-dsl-jobs', root='http://saltstack.github.io/jenkins-dsl-jobs/')
@Grab('com.saltstack:jenkins-dsl-jobs:1.0-SNAPSHOT')

import javax.mail.internet.InternetAddress
import com.saltstack.jenkins.EmailNotifications

recipients = [new InternetAddress(build.getEnvVars()["ghprbActualCommitAuthorEmail"])]
// Filter opt-out addresses
opt_out_filtered = recipients.findAll { addr -> EmailNotifications.opt_out.contains(addr.toString()) }
msg.setRecipients(javax.mail.Message.RecipientType.TO, opt_out_filtered as javax.mail.Address[])
