Thank you for downloading Payara Blue 4.1.2.184!

Here are a few short steps to get you started...

Payara Blue currently supports the following Java Virtual Machines:

* IBM JDK 7 SR9 FP40 or higher
* IBM JDK 7 Release 1 SR3 FP40 or higher
* IBM JDK 8 SR3 or higher

1. Installing Payara Blue
===========================

Installing Payara Server is just a matter of unzipping the Payara Server archive in the desired directory. Since you are reading this, you have probably already unzipped Payara Server. If not, just type the following command in the directory where you want Payara Server to be installed: jar xvf payara-4.1.2.184.zip

The default domain called 'domain1' is already installed and preconfigured, along with a JDK 8 configured domain 'payaradomain'.


2. Starting Payara Blue
=======================

The 'asadmin' command-line utility is used to control and manage Payara Blue (start, stop, configure, deploy applications, etc.).

To start Payara Blue, just go in the directory where Payara Blue is located and type:
        On Unix: payara41/bin/asadmin start-domain
        On Windows: payara41\bin\asadmin start-domain

After a few seconds, Payara Blue will be up and ready to accept requests. The default 'domain1' domain is configured to listen on port 8080. In your browser, go to http://localhost:8080 to see the default landing page.

To manage Payara Blue, just go to web administration console: http://localhost:4848


3. Stopping Payara Blue
=======================

To stop Payara Blue, just issue the following command:
        On Unix: payara41/bin/asadmin stop-domain
        On Windows: payara41\bin\asadmin stop-domain


4. Where to go next?
====================

Open the following in your browser: https://docs.payara.fish/. It contains useful information such as the details about the Payara Project, links to the Payara Server Documentation, etc.


Make sure to also check the Payara Server 4.1.2.184 Release Notes as they contain important information: https://docs.payara.fish/v/4.184/release-notes/release-notes-184.html


5. Documentation 
================

Payara Server 4.1.2.184 Release Notes: https://docs.payara.fish/v/4.184/release-notes/release-notes-184.html

Payara Server Documentation: https://docs.payara.fish/

Payara GitHub Project: https://github.com/payara/Payara

Payara Community: http://www.payara.fish/community 


6. Follow us
============

Make sure to follow @Payara_Fish on Twitter and read The Payara Blog (http://blog.payara.fish/) to get the latest news on Payara.
