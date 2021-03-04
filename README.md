<a href="https://scan.coverity.com/projects/rlp_01">
  <img alt="Coverity Scan Build Status"
       src="https://scan.coverity.com/projects/22710/badge.svg"/>
</a>
rlp-01

This is a minimal RELP implementation. It has a RelpSession class, which is
an API to RELP. Binary data transmission is supported.
For more information, see the specification on the internal wiki

Usage example:

RelpSession relpSession = new RelpSession(hostname, port);
// now session is open
byte[] syslogMsg = "Hello, world".getBytes("US-ASCII");
relpSession.sendMessage(syslogMsg);
relpSession.close();


enable debug with export RELP_DEBUG=1
