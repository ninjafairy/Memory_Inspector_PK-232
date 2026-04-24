### 3Rdparty ON|OFF                                             Default: OFF
**Mode:** Packet/MailDrop                                       Host: 3R
**Parameters:**
- ON    -   The MailDrop will handle third party traffic.
- OFF   -   The MailDrop will only handle mail to or from MYCALL or MYMAIL.
**Description:**
If 3RDPARTY is ON, then remote MailDrop users may leave messages for any station. _______________________________________________________________________________

---

### 5Bit                                                   Immediate Command
**Mode:** Command                                          Host: 5B
**Description:**
_______________________________________________________________________________ 5BIT is an immediate command allowing the user to store 5-bit Baudot  transmissions then write a program to decrypt codes that involve bit inversion  or transposition.  In 5BIT, a constant of $40 (64 decimal) is added to each  received 5-bit character to make it a printable ASCII character in the range of  $40-5F.  All characters are treated this way, including CR, LF, LTRS and FIGS. When the user enters 5BIT, the PK-232 displays "OPMODE now BAUDOT".  This is  not strictly true; however, the BAUDOT LED on the front panel will be lit. RXREV, RBAUD and WIDESHFT must be set properly for the monitored transmission.   SIGNAL is helpful in determining whether a transmission is 5-bit and the settings  for RBAUD and RXREV, but typing OK after SIGNAL selects BAUDOT, not 5BIT. Do not change modes directly between BAUDOT, 5BIT and 6BIT.  Go through some  other mode first, such as Packet.  These commands do not function in 5BIT: BITINV, CCITT, CODE, MARSDISP, TRACE, USOS, WRU and XMIT. _______________________________________________________________________________

---

### 6Bit                                                   Immediate Command
**Mode:** Command                                          Host: 6B
**Description:**
_______________________________________________________________________________ Same as 5BIT, except that the unit receives a 6-bit code and adds a constant of  $30 (48 decimal) to yield a range of $30-6F.  RXREV, RBAUD and WIDESHFT must be  set correctly.  SIGNAL is helpful in determining whether a transmission is 6-bit  and the settings for RBAUD and RXREV, but OK will not automatically select 6BIT. When the user enters 6BIT, the PK-232 displays "OPMODE now BAUDOT".  This is  not strictly true; however, the BAUDOT LED on the front panel will be lit. Do not change modes directly between BAUDOT, 5BIT and 6BIT.  Go through some  other mode first, such as Packet.  These commands do not function in 6BIT: BITINV, CCITT, CODE, MARSDISP, TRACE, USOS, WRU or XMIT.

_______________________________________________________________________________

---

### 8Bitconv ON|OFF                                             Default: OFF
**Mode:** Packet/ASCII                                          Host: 8B
**Parameters:**
- ON   -    The high-order bit IS NOT stripped in Converse Mode.
- OFF  -    The high-order bit IS stripped in Converse Mode.
**Description:**
8BITCONV permits packet and ASCII transmission of 8-bit data in Converse Mode. When 8BITCONV is OFF (default), the high-order bit (bit seven) of characters  received from the terminal is set to 0 before the characters are transmitted. _______________________________________________________________________________

---

### AAb text                                               Default: empty
**Mode:** Baudot, ASCII, AMTOR                             Host: AU
**Parameters:**
- text  -   Any combination of characters up to a maximum of 24 characters.
**Description:**
Use the AAB command to enter an acknowledgment text for Baudot, ASCII and AMTOR.   AAB sends automatic confirmation in Baudot, ASCII and AMTOR in response to a  distant station's WRU? request.  Set WRU ON or CUSTOM bit-9 to activate your  Auto-AnswerBack. Type "AAB (24-character text)" to store your AnswerBack in memory. _______________________________________________________________________________

---

### ABaud "n"                                              Default: 110 bauds
**Mode:** ASCII                                            Host: AB
**Parameters:**
- "n"   -   Specifies the ASCII data rate or signaling speed in bauds from your 
- PK-232 to your radio.
**Description:**
ABAUD sets the radio ("on-air") baud rate only in the ASCII operating mode.   This value has no relationship to your computer or terminal program's baud rate. The available "n" ASCII data rates in bauds are: 45, 50, 57, 75, 100, 110, 150, 200, 300, 400, 600, 1200, 2400, 4800 and 9600 _______________________________________________________________________________

---

### AChg                                                   Immediate Command
**Mode:** AMTOR                                            Host: AG
**Description:**
_______________________________________________________________________________ ACHG is an immediate command used in AMTOR by the receiving station to interrupt  the sending station's transmissions. As the receiving station, you usually rely on the distant station, your partner  in the ARQ "handshake", to send the "+?" command to do the changeover.  However,  in ARQ (Mode A), you can use the ACHG command to "break in" on the sending  station's transmission.  Use the ACHG command only when it is needed.

_______________________________________________________________________________

---

### ACKprior ON|OFF                                             Default: OFF
**Mode:** Packet                                                Host: AN
**Parameters:**
- ON    -   Priority Acknowledgment is enabled.
- OFF   -   This feature is disabled.
**Description:**
This command implements the Priority Acknowledge scheme described by Eric  Gustafson (N7CL), which proposes to improve multiple access packet performance  on HF and VHF simplex channels with hidden terminals.  When a busy channel  clears, the acknowledgments are sent immediately, while data and poll bits are  held off long enough to prevent collisions with the ACKs.  By giving priority to  data ACKs, fewer ACKs will collide with other station's data, reducing retries.   Digipeated frames are sent immediately.  RAWHDLC and KISS force ACKPRIOR off. These are the defaults for a P-persistence system with NO Priority Acknowledgment: ACKPRIOR OFF,  PPERSIST ON,   PERSIST  63,  SLOTTIME 30, RESPTIME 0,    MAXFRAME 4,    FRACK    5 The following are the recommended command settings for Priority Acknowledge: 1200 baud VHF packet                              300 baud HF packet ACKPRIOR ON                                      ACKPRIOR ON PPERSIST ON                                      PPERSIST ON PERSIST  84                                      PERSIST  84 SLOTTIME 30                                      SLOTTIME 8 RESPTIME 0                                       RESPTIME 0 MAXFRAME 1 - 7                                   MAXFRAME 1 FRACK    8                                       FRACK    15 HBAUD    1200                                    HBAUD    300 VHF      ON  (or TONE 3)                         VHF      OFF (or TONE 0) DWAIT    doesn't matter                          DWAIT    doesn't matter Stations using neither the Priority Acknowledge nor the P-persistence schemes  should set DWAIT 73 for 1200 baud and DWAIT 76 for 300 baud work.  Stations  using P-persistence but not Priority Acknowledge should set PERSIST and SLOTTIME  to the same values that ACKPRIOR stations are using. Timewave and TAPR use some different command names to handle P-persistence. The following table should help with the AEA/TAPR command differences: TAPR_SLOTS   MFJ SLOTMASK     AEA_PERSIST   Remarks 1              $00            255       Disables slotting 2              $01            127 3                              84 4              $03             63       Default setting  6                              42 8              $07             31       Very busy channel 12                             20 16             $0F             15       Extremely busy channel 64             $3F              3 Timewave products calculate the TAPR ACKTIME value based on the setting of HBAUD.   The TAPR DEADTIME command is analogous to the Timewave SLOTTIME command.

_______________________________________________________________________________

---

### ACRDisp "n"                                                 Default: 0
**Mode:** ALL                                                   Host: AA
**Parameters:**
- "n"   -   0 to 255 specifies the screen width, in columns or characters.
- 0 (zero) disables this function.
**Description:**
The numerical value "n" sets the terminal output format for your needs.  Your  PK-232 sends a <CR><LF> sequence to your computer or terminal at the end of a  line in the Command and Converse Modes when "n" characters have been printed.   Most computers and terminals do this automatically so ACRDISP defaults to 0. When the PK-232 is in the MORSE mode, received data will be broken up on word  boundaries if possible.  At a column of 12 less than the ACRDISP value, the  PK-232 starts looking for spaces in the received data.  The first space received  after this column forces the PK-232 to generate a carriage return.  If ACRDISP  is 0 (default), this occurs at column 60.  If there are no spaces at or after  this column then a carriage return occurs at ACRDISP. _______________________________________________________________________________

---

### ACRPack ON|OFF                                              Default: ON
**Mode:** Packet                                                Host: AK
**Parameters:**
- ON   -    The send-packet character IS added to packets sent in Converse Mode.
- OFF  -    The send-packet character is NOT added to packets.
**Description:**
When ACRPACK is ON (default), all packets sent in Converse Mode include the  SEND-PACket character (normally <CR>) as the last character of the packet. When ACRPACK is OFF, the send-packet character is interpreted as a command, and  is not included in the packet or echoed to the terminal. ACRPACK ON and SENDPAC $0D produce a natural conversational mode. _______________________________________________________________________________

---

### ACRRtty "n"                                       Default: 71 (69 in AMTOR)
**Mode:** Baudot/ASCII RTTY                          Host: AT
**Parameters:**
- "n" -     0 - 255 specifies the number of characters on a line before a carriage 
- return <CR> is automatically inserted in your transmitted text.
- Zero (0) disables the function.
**Description:**
When sending Baudot or ASCII RTTY, the ACRRTTY feature automatically sends a  carriage return at the first space following the "nth" character or column.  Use this option when you are sending and don't want to be bothered by watching  the screen or worrying about line length.  You should NOT use this option when  retransmitting text received from another station; for example, ARRL Bulletins. ACRRTTY is used in AMTOR, except that AMTOR is limited by international telex  practices to a maximum of 69 characters per line.  If ACRRTTY is set to 71, in  AMTOR the automatic carriage return function operates after 69 characters.

_______________________________________________________________________________

---

### ADDress "n"                                            Default: $0000
**Mode:** ALL                                              Host: AE
**Parameters:**
- "n"  -    Zero to 65,535 ($0 to $FFFF) setting an Address in the PK-232 memory.
**Description:**
The ADDRESS sets an address somewhere in the PK-232's memory map.  This command  is usually used with the IO, MEMORY and the PK commands.  It is used primarily  by programmers and is of little use without the PK-232 Technical Manual. _______________________________________________________________________________

---

### ADelay "n"                                        Default: 4 (40 msec.)
**Mode:** AMTOR                                       Host: AD
**Parameters:**
- "n"  -    1 to 9 specifies transmitter key-up delay in 10-millisecond intervals.
**Description:**
ADELAY is the length of time in tens of milliseconds between the time when the  PK-232 activates the transmitter's PTT line and the ARQ data begins to flow.   The ADELAY command allows you to adjust a variable delay, from 10 to 90  milliseconds to handle the PTT (Push-to-Talk) delay of different transmitters. In most cases, the default value of 4 (40 milliseconds) is adequate for the  majority of the popular HF transmitters.  If the AMTOR signal strength is good  and you observe periodic errors caused by loss of phasing (shown by rephase  cycles in the middle of an ARQ contact) during contacts, it may be necessary to  adjust the ADELAY value. o    Be sure that errors and rephasing effects are not caused by the distant  station before changing your ADELAY. o    If changing your ADELAY values does not improve link performance, reinstall  your original value and ask the other station to try changing his ADELAY. Because the ARQ mode allows 170 milliseconds for the signal to travel to the  distant station and return, increasing ADELAY will reduce the maximum working  distance.  The maximum theoretical range of an ARQ contact is limited to about  25,500 kilometers.  Using some of that time as transmit delay leaves less time  for signal propagation.  Thus the maximum distance available is reduced. Regardless of the setting of ADELAY, ARQ (Mode A) AMTOR may not work very well  over very short distances, e.g., one or two miles.  However, in very short  distance work, ARQ should not be necessary to achieve error-free copy. _______________________________________________________________________________

---

### AFilter ON|OFF                                              Default: OFF
**Mode:** ALL                                                   Host: AZ
**Parameters:**
- ON  -     The ASCII characters specified in the MFILTER are filtered out and 
- never sent to the terminal or computer.
- OFF -     Characters in MFILTER list are only filtered from monitored packets.
**Description:**
Some terminals and computers use special characters to clear the screen or  perform other "special" functions.  Placing these characters in the MFILTER  list, and turning AFILTER ON will keep the PK-232 from sending them. Exception:  When ECHO is ON, and the terminal or computer sends a filtered  character, the PK-232 will echo it back to the terminal or computer. AFILTER works regardless of mode, or CONNECT/CONVERSE/TRANSPARENT status.   One must be careful to leave AFILTER OFF during Binary file transfers. _______________________________________________________________________________

---

### ALFDisp ON|OFF                                              Default: ON
**Mode:** All                                                   Host: AI
**Parameters:**
- ON   -    A line feed character <LF> IS sent to the terminal after each <CR>.
- OFF  -    A <LF> is NOT sent to the terminal after each <CR>.
**Description:**
ALFDISP controls the display of carriage return characters received, as well as  the echoing of those that are typed in. When ALFDISP is ON (default), your PK-232 adds a line feed <LF> to each carriage  return <CR> received, if needed.  If a line feed was received either immediately  before or after a carriage return, ALFDISP will not add another line feed.  Use  the PK-232's sign-on message to determine how carriage returns are being  displayed.  ALFDISP affects your display; it does not affect transmitted data. Set ALFDISP ON if the PK-232's sign-on message lines are typed over each other. Set ALFDISP OFF if the PK-232's sign-on message is double spaced. ALFDISP is set correctly if the PK-232's sign-on message is single spaced.   _______________________________________________________________________________

---

### ALFPack ON|OFF                                              Default: OFF
**Mode:** Packet                                                Host: AP
**Parameters:**
- ON   -    A <LF> character IS added after each <CR> sent in outgoing packets.
- OFF  -    A <LF> is NOT added to outgoing packets (default).
**Description:**
ALFPACK is similar to ALFDISP, except that the <LF> characters are added to  outgoing (transmitted) packets, rather than to text displayed locally. o    If the person you are talking to reports overprinting of packets from your  station, set ALFPACK ON.  ALFPACK is disabled in Transparent Mode.

_______________________________________________________________________________

---

### ALFRtty ON|OFF                                              Default: ON
**Mode:** Baudot/ASCII RTTY                                     Host: AR
**Parameters:**
- ON   -    A line feed character <LF> IS sent after each carriage return <CR>.
- OFF  -    A <LF> is NOT sent after each <CR>.
**Description:**
If ALFRTTY is set ON when transmitting Baudot or ASCII RTTY, a line feed  character is added and sent automatically after each <CR> character you type. Use this option when you are typing into the transmit buffer and don't want to  be bothered worrying about line length.  You should NOT use this option when  retransmitting text received from another station; for example, ARRL Bulletins. o    ALFRTTY has no effect in AMTOR; a line feed is automatically added after  each carriage return. _______________________________________________________________________________

---

### AList                                                  Immediate Command
**Mode:** AMTOR                                            Host: AL
**Description:**
_______________________________________________________________________________  ALIST is an immediate command that switches your PK-232 into the ARQ Listen mode. You can usually monitor a contact between two linked stations using the ARQ  Listen mode (also called Mode L).  This mode may need a few seconds to phase or  acquire synchronization with the other stations.  Your ability to synchronize  with the master station depends on operating conditions such as interference.   Your monitor will display all the retries if the linked stations that you are  monitoring are experiencing ARQ errors. Type ALIST (or AL) repeatedly if you lose synchronization. _______________________________________________________________________________

---

### ALTModem "n"                                                Default: 0
**Mode:** Packet                                                Host: Am
**Parameters:**
- "n"   -   A numeric value of 0 or 1 selecting either the default (0) PK-232 
- 45-300/1200 baud modem, or the optional (1) 2400 bps DPSK modem.
**Description:**
n is a numeric value 0 or 1, selecting the standard PK-232 modem (ALTMODEM 0,  default) or the optional 2400 bps modem (ALTMODEM 1).  Only the Packet mode uses  ALTMODEM 1.  In all other modes, one of the internal FSK/CW modems is selected. ALTMODEM is used only when the optional internal 2400 bps DPSK modem has been  factory installed.  Remember to set HBAUD to 2400 when using this modem.

_______________________________________________________________________________

---

### AMtor                                                  Immediate Command
**Mode:** Command                                          Host: AM
**Description:**
_______________________________________________________________________________ AMTOR is an immediate command that switches your PK-232 into the AMTOR mode.   Your PK-232 is automatically placed in ARQ Standby condition. Your station is then available for automatic access by and response to any AMTOR  station that sends your SELCALL.  The PK-232 can communicate using either the  CCIR 476 (4-character SELCALL) or the CCIR 625 (7-character SELCALL) protocol.   Your monitor will also display any inbound FEC (Mode B) transmissions. See the MYSELCAL and MYIDENT commands to enter your 4 and 7 character SELCALLs. _______________________________________________________________________________

---

### ARq aaaa[aaa]                                          Immediate Command
**Mode:** AMTOR                                            Host: AC
**Parameters:**
- aaaa[aaa]   -  The distant station's 4-character or 7-character SELCALL code.
**Description:**
ARQ is an immediate command that starts an AMTOR Mode A (ARQ) SELCALL (SELective  CALL) to a distant station. To begin the Mode A (ARQ) selective call type "ARQ" followed by the other  station's SELCALL: Example:  ARQ NNML            (4-character SELCALL) or        ARQ VTMFFFF         (7-character SELCALL) As soon as a <CR> is typed, your PK-232 will begin keying your transmitter in  the three-character AMTOR ARQ burst sequence.  If the distant station receives  and decodes your selective call successfully, the two AMTOR systems synchronize  and begin the Mode A (ARQ) AMTOR "handshaking" process. See the MYSELCAL and MYIDENT commands to enter your 4- and 7- character SELCALLs. Other AMTOR commands are ACHG, ACRRTTY, ADELAY, ALFRTTY, ARQTMO, EAS, HEREIS and  RECEIVE.

---

### ARQE  Immediate Command 
**Mode:** Command  Host: As
**Description:**
ARQE is an immediate command that switches the PK-232 into the ARQ-E receiving mode. ARQ-E is similar to 1-channel TDM, except that the 7-bit code is different. Like TDM most ARQ-E stations send idle signals for long periods of time.  The PK-232 can only phase on ARQ-E signals that are idling so this is not a problem. The SIGNAL Identification (SIAM) mode will identify ARQ-E signals for the user. They are identified as "TDM ARQ-E:4" or 'TDM ARQ-E:S,' referring to 4- and 8character repetition cycles used in this mode.

---

### ARQTmo "n" Default: 60
**Mode:** AMTOR, Pactor Host: AO
**Parameters:**
- “n”   -      0 to 250 specifies the number of seconds to send an ARQ SELCALL
- before automatic transmitter shutdown.
- ARQTMO sets the length of time during which your ARQ call will be sent, shutting
- down automatically.  As a general rule, if you can't activate another AMTOR or
- Pactor station in the default time of 60 seconds, you can probably assume that
- the other station can't hear your transmission.
- ARQTOL “n" Default: 3
- Mode: AMTOR ARQ Host: AO
- Parameters:
- “n”'   1 to 5, specifying a relative tolerance for bit boundary jitter.
- ARQTOL controls the tolerance for received bit boundary jitter in AMTOR ARQ mode.
- n is a number from 1 (tight tolerance) to 5 (loose tolerance).  The number
- signifies how far away from the expected bit transition time the actual received
- transition may be, in tenths of a bit (milliseconds).  If the transition occurs
- further away than expected, the received block is counted as an error, even if
- all three characters in the block appear to be valid AMTOR characters.  The
- default value of ARQTOL 3 is the equivalent of the fixed tolerance of previous
- firmware releases.
- ARQTOL should be set to a low number (tighter tolerance) for applications that
- require nearly error-free communications.  The tradeoff is that good received
- character blocks are counted as bad if the bit transitions are suspect, thereby
- causing retransmissions and lowering the effective character rate.
- ARQTOL does not affect FEC, SELFEC or ARQ Listen modes.
- Ascii Immediate Command
- Mode: Command Host: AS
- ASCII is an immediate command that switches your PK-232 into the ASCII mode.
- ASCII is the proper mode to use if you wish to use RTTY to transmit text, data or
- other information containing lower case and special characters not present in the
- Baudot/Murray and ITA #2 alphabets or character sets.  When 8BITCONV is set ON,
- 8-bit ASCII data may also be sent and received.
- Because the ASCII character set requires a minimum of seven bits to define each
- character, under worst-case conditions, ASCII is sometimes more subject to data
- errors and garbled text than Baudot/ITA#2 at the same data rate.
**Description:**


---

### ARQTOL “n" Default: 3
**Mode:** AMTOR ARQ Host: AO
**Parameters:**
- “n”'   1 to 5, specifying a relative tolerance for bit boundary jitter.
- ARQTOL controls the tolerance for received bit boundary jitter in AMTOR ARQ mode.
- n is a number from 1 (tight tolerance) to 5 (loose tolerance).  The number
- signifies how far away from the expected bit transition time the actual received
- transition may be, in tenths of a bit (milliseconds).  If the transition occurs
- further away than expected, the received block is counted as an error, even if
- all three characters in the block appear to be valid AMTOR characters.  The
- default value of ARQTOL 3 is the equivalent of the fixed tolerance of previous
- firmware releases.
- ARQTOL should be set to a low number (tighter tolerance) for applications that
- require nearly error-free communications.  The tradeoff is that good received
- character blocks are counted as bad if the bit transitions are suspect, thereby
- causing retransmissions and lowering the effective character rate.
- ARQTOL does not affect FEC, SELFEC or ARQ Listen modes.
- Ascii Immediate Command
- Mode: Command Host: AS
- ASCII is an immediate command that switches your PK-232 into the ASCII mode.
- ASCII is the proper mode to use if you wish to use RTTY to transmit text, data or
- other information containing lower case and special characters not present in the
- Baudot/Murray and ITA #2 alphabets or character sets.  When 8BITCONV is set ON,
- 8-bit ASCII data may also be sent and received.
- Because the ASCII character set requires a minimum of seven bits to define each
- character, under worst-case conditions, ASCII is sometimes more subject to data
- errors and garbled text than Baudot/ITA#2 at the same data rate.
**Description:**


---

### Ascii Immediate Command
**Mode:** Command Host: AS
**Description:**
ASCII is an immediate command that switches your PK-232 into the ASCII mode. ASCII is the proper mode to use if you wish to use RTTY to transmit text, data or other information containing lower case and special characters not present in the Baudot/Murray and ITA #2 alphabets or character sets.  When 8BITCONV is set ON, 8-bit ASCII data may also be sent and received. Because the ASCII character set requires a minimum of seven bits to define each character, under worst-case conditions, ASCII is sometimes more subject to data errors and garbled text than Baudot/ITA#2 at the same data rate.

_______________________________________________________________________________

---

### ASPect  "n"                                            Default: 2 (576)
**Mode:**  FAX                                             Host: AY
**Parameters:**
- "n"   -   1 to 6, specifying the number of FAX scan lines the PK-232 prints out 
- of every 6 lines received.
**Description:**
ASPECT controls the aspect ratio of the length to the width of a FAX image by  controlling the number of lines the PK-232 prints out of each 6 received lines.   On most weather charts, the default of ASPECT 2 keeps the shapes received in the  right proportion.  On other transmissions, you may want more resolution. See the table below for suggested settings. CCITT IOCs for narrow and wide carriage printers are given for each ASPECT  setting below. ASPECT    CCITT IOC (narrow)                 CCITT IOC (wide) 1             1100                              1788 2              550  (Weather Charts 576)         894 3              367  (Wirephotos 352)             596  (Weather Charts 576)  4              275  (WEFAX Satellite 288)        447 5              220                               358  (Wirephotos 352) 6              183                               298  (WEFAX Satellite 288) The Index Of Cooperation, or IOC is an international measure of aspect ratio.   The formula for the CCITT IOC is: (vertical scan line density) X (horizontal width) _________________________________________________ 3.14159 Weather charts are transmitted at a nominal CCITT IOC of 576.  ASPECT 2 is so  close to this that the charts print with no noticeable distortion. Please note that high settings of ASPECT such as 6 may generate data so often  that your printer cannot handle the high data rate.

_______________________________________________________________________________

---

### AUdelay "n"                                       Default: 2 (20 msec.)
**Mode:** Baudot, ASCII, FEC, FAX and Packet          Host: AQ
**Parameters:**
- "n"  -    0 - 120 specifies in units of 10 msec. intervals, the delay between 
- PTT going active and the start of the transmit AFSK audio tones.
**Description:**
In some applications it may be desirable to create a delay from the time that  the radio PTT line is keyed and the time that audio is produced from the PK-232.   Most notably, on HF when an amplifier is used, arcing of the amplifier relay  contacts may occur if drive to the amplifier is applied before the contacts have  closed.  If arcing occurs, increase AUDELAY slowly until the arcing stops.   In VHF or UHF FM operation, some synthesized transceivers may produce  undesirable spurious emissions, if audio and PTT are applied at the same time.   These emissions may be reduced by setting AUDELAY to roughly 1/2 of TXDELAY. Please note that AUDELAY must always be less than TXDELAY.  It is advisable that  AUDELAY be set lower than TXDELAY by a setting of 10.  For example, you have  determined that a TXDELAY of 20 works well for your transceiver.  Subtracting 10  from 20 yields 10, which is the recommended setting for AUDELAY.  If a setting  of AUDELAY of 10 is too short, then set both TXDELAY and AUDELAY higher. _______________________________________________________________________________

---

### AUTOBaud ON|OFF                                             Default: OFF
**Mode:** Command                                               Host: Ab
**Parameters:**
- ON  -     Autobaud Routine always present at Power-ON or RESTART.
- OFF -     Autobaud Routine active at Power-ON only if battery jumper is removed.
**Description:**
When AUTOBAUD is OFF (default), the unit performs the autobaud function only  when powering ON or after a RESET.  When AUTOBAUD is ON, the PK-232 performs the  autobaud routine EVERY time it is powered ON, and EVERY time the RESTART command  is entered.  The stored parameters (e.g. MYCALL) are saved if the battery jumper  is connected.  The unit displays the autobaud message at the same rate as the  last setting of TBAUD.  AUTOBAUD ON is helpful when moving the unit from one  computer to another, where the terminal data rates are different. In the autobaud routine, only one asterisk (*) is needed to set the terminal  speed TBAUD.  The autobaud routine detects 110, 300, 600, 1200, 2400, 4800 and  9600 baud, at either 7 bits even parity, or 8 bits no parity.

_______________________________________________________________________________

---

### AWlen "n"                                                   Default: 7
**Mode:** All                                                   Host: AW
**Parameters:**
- "n"  -    7 or 8 specifies the number of data bits per word.
**Description:**
The parameter value defines the digital word length used by the serial  input/output (I/O) terminal port and your computer or terminal program. AWLEN will probably be set properly by the PK-232 Autobaud routine.  Still  you may want to change the ASCII word-length at some time to accommodate a  terminal program you wish to use. For plain text conversations with the PK-232, an AWLEN of 7 or 8 may be used.   For binary file transfers and HOST Mode operation, an AWLEN of 8 MUST be used. The RESTART command must be issued before a change in word length takes effect. Do NOT change AWLEN unless the terminal can be changed to the same setting. _______________________________________________________________________________

---

### Ax25l2v2 ON|OFF                                             Default: ON
**Mode:** Packet                                                Host: AV
**Parameters:**
- ON   -    The PK-232 uses AX.25 Level 2 Version 2.0 protocol.
- OFF  -    The PK-232 uses AX.25 Level 2 Version 1.0 protocol.
**Description:**
This command allows the selection of either the old (version 1) version of the  AX.25 packet protocol or the current (version 2.0) protocol.  Some  implementations of version 1 of AX.25 protocol won't properly digipeat Version  2.0 AX.25 packets.  Most users run AX.25 version 2 but this command allows  returning to the older version if necessary for compatibility. _______________________________________________________________________________

---

### AXDelay "n"                                       Default: 0 (00 msec.)
**Mode:** Packet                                      Host: AX
**Parameters:**
- "n"  -    0 to 180  specifies a key-up delay for voice repeater operation in 
- ten-millisecond intervals.
**Description:**
AXDELAY specifies the period of time the PK-232 will wait - in addition to the  delay set by TXDELAY - after keying the transmitter and before data is sent. Packet groups using a standard "voice" repeater to extend the range of the local  area network may need to use this feature. Repeaters with slow electromechanical relays, auxiliary links (or other circuits  which delay transmission after the RF carrier is present) require more time to  get RF on the air.  Try various values to find the best value for "n" if you're  using a repeater that hasn't been used for packet operations before.  If other  packet stations have been using the repeater, check with them for the proper  setting.  AXDELAY acts together with AXHANG.

_______________________________________________________________________________

---

### AXHang "n"                                        Default: 0 (000 msec.)
**Mode:** Packet                                      Host: AH
**Parameters:**
- "n"  -    0 to 20 specifies voice repeater "hang time" in 100-millisecond 
- intervals.
**Description:**
AXHANG allows you to increase efficiency when sending packets through an audio  repeater that has a hang time greater than 100 milliseconds. When the PK-232 has heard a packet sent within the AXHANG period, it does not  add the repeater keyup delay (AXDELAY) to the key-up time.  Try various values  to find the best value for "n" if you are using a repeater that hasn't been used  for packet operations before.  If other packet stations have been using the  repeater, check with them for the proper setting. _______________________________________________________________________________

---

### BAudot                                                 Immediate Command
**Mode:** Command                                          Host: BA
**Description:**
_______________________________________________________________________________ BAUDOT is an immediate command that switches the PK-232 into the Baudot mode. Baudot RTTY operation is very common around the world, and is the basis of the  telex network and most radio press, weather and point-to-point message services. The Baudot/Murray and ITA #2 character sets do not contain lower case or the  special punctuation and control characters found in ASCII.  Because the  Baudot/ITA #2 code requires only five information bits to define each character,  it will generally suffer fewer errors than ASCII code at the same data rate. _______________________________________________________________________________

---

### BBSmsgs ON|OFF                                              Default: OFF
**Mode:** Packet                                                Host: BB
**Parameters:**
- ON  -     Makes the PK-232 status messages look like the TAPR-style output.
- OFF -     The PK-232 status messages work as before (default).
**Description:**
When BBSMSGS is ON, some of the status messages change or are suppressed which  may improve operation of the PK-232 with some BBS software.  The following AEA  PK-232 status messages are suppressed or changed if BBSMSGS is ON: No "(parm) was (value)" No "(parm) now (value)"           Connect messages: No "; v2; 1 unACKed" No "xxx in progress: (dest) via (digis)" No space after comma in digipeater lists "VIA" in upper case If MRPT is ON, digi paths are displayed in TAPR format No "*** connect request:" No "*** retry count exceeded" Sends carriage return before all other "***" No "(callsign) busy" message

_______________________________________________________________________________

---

### Beacon EVERY|AFTER "n"                       Default: EVERY 0 (00 sec.)
**Mode:** Packet                                 Host: BE
**Parameters:**
- EVERY  -  Send the beacon at regular intervals.
- AFTER  -  Send the beacon after the specified time interval without activity.
- "n"    -  0 to 250 sets beacon timing in ten-second intervals.
- "0"    -  Zero turns off the beacon (default).
**Description:**
The BEACON command sets the conditions under which your beacon will be sent. A beacon frame contains the text that you've typed into the BTEXT message in a  packet addressed to the UNPROTO address.  When the keyword EVERY is specified a  beacon packet is sent every "n" times ten seconds.  When AFTER is specified, a  beacon is sent after "n" times ten seconds have passed without packet activity. If you set the BEACON timing less than "90" - a value judged as too short for  busy channels - you'll see the following message at each command prompt: WARNING: BEACON too often _______________________________________________________________________________

---

### BItinv  "n"                                                 Default: $00
**Mode:**  RTTY                                                 Host: BI
**Parameters:**
- "n"   -   0 to $1F, (0 to 31 decimal) specifies a number to be exclusive-ORed 
- with every received Baudot character.  BITINV 0 is plain text.
**Description:**
Bit inversion is used to prevent listeners from reading some commercial Baudot  transmissions.  Usually either 2 or 3 bits of each character are inverted to  give the appearance of an encrypted transmission.  Try different settings of  BITINV on a Baudot signal after the baud rate has been determined.  If you are  interested encrypted transmissions try experimenting with the 5BIT command. _______________________________________________________________________________

---

### BKondel ON|OFF                                              Default: ON
**Mode:** All                                                   Host: BK
**Parameters:**
- ON   -    The sequence <BACKSPACE><SPACE><BACKSPACE> is echoed when a character 
- is deleted from the input line.
- OFF  -    The <BACKSLASH> character <\> is echoed when a character is deleted.
**Description:**
BKONDEL determines how character deletion is displayed in Command or Converse  mode.  When BKONDEL is ON (default) the <BACKSPACE><SPACE><BACKSPACE> sequence  is produced which updates the video display screen erasing the character. On a printing terminal the <BACKSPACE><SPACE><BACKSPACE> sequence will result in  overtyped text.  Set BKONDEL OFF if you have a paper-output display, or if your  terminal does not respond to the <BACKSPACE> character <CTRL-H>.  When BKONDEL  is OFF the PK-232 displays a <BACKSLASH> for each character you delete.  You can  get a display of the corrected input by typing the REDISPLAY-line character.

_______________________________________________________________________________

---

### BText text                                                  Default: empty
**Mode:** Packet                                                Host: BT
**Parameters:**
- text  -   Any combination of characters up to a maximum length of 120 characters.
**Description:**
BTEXT is the content of the data portion of a beacon packet.  The default text  is an empty string (no message).  When and how packet beacons are sent is  discussed in more detail under the BEACON command. Although the beacon subject is controversial in packet circles, you can use  beacon texts intelligently and benefit the packet community. o    Don't type your call sign in BTEXT - the normal packet header shows it. o    Don't fill BTEXT with screen graphics.  Use BTEXT for meaningful information. o    After you've beaconed for a week or two and people know who you are, follow  the practice used by more experienced packeteers:  SET BEACON EVERY 0! o    Use a "%," "&", "N," "NO," "NONE," or OFF as the first characters in the  text to clear the BTEXT text. _______________________________________________________________________________

---

### CALibrate                                              Immediate Command
**Mode:** Command                                          Host: Not Supported
**Description:**
_______________________________________________________________________________ CALIBRATE is an immediate command that starts the AFSK transmit tone calibration. The PK-232 provides a continuous on-screen display of AFSK generator tone  frequencies in Hertz.  The frequency is displayed approximately twice per  second, with the part number of the potentiometer associated with that tone. When Calibration is checked all packet connections will be lost, and the time- of-day clock will not advance until you quit the calibration routine. Commands available in the calibration routine are: K         Toggles the PK-232's PTT and CW keying outputs between ON and OFF. Q         Quits the calibration routine. H         Toggles the generator between wide (1000 Hz) and narrow (200 Hz) shift. <SPACE>   Toggles the audio tone between "mark" (low) and "space" (high) tones. D         Toggles between transmitting a continuous tone or alternating the mark  and space tones at a rate set by the radio baud (HBAUD) rate. FREQUENCY      RESISTOR                 FUNCTION 2310           R164             HF SPACE, RTTY SPACE _____________________________________________________________________ 2200           R165             VHF SPACE, WIDESHIFT SPACE _____________________________________________________________________ 1200           R167             VHF MARK, CW AFSK, WIDESHIFT MARK This tone should be adjusted before R168 _____________________________________________________________________ 2110           R168             HF MARK, RTTY MARK _____________________________________________________________________

_______________________________________________________________________________

---

### CANline "n"                                       Default: $18 <CTRL-X>
**Mode:** All                                         Host: CL
**Parameters:**
- "n"  -    0 to $7F (0 to 127 decimal) specifies an ASCII character code.
**Description:**
The parameter "n" is the ASCII code for the character you want to use to cancel  an input line.  You can enter the code in either hex or decimal. When you use the CANLINE character to cancel an input line in Command Mode, the  line is terminated with a <BACKSLASH> character and a new prompt (cmd:) appears. When you cancel lines in Converse Mode, only a <BACKSLASH> and a new line appear. o    You can cancel only the line you are currently typing. o    Once <CR> or <Enter> has been typed, you cannot cancel an input line. NOTE:     If your send-packet character is not <CR> or <Enter>, the cancel-line  character cancels only the last line of a multi-line packet. _______________________________________________________________________________

---

### CANPac "n"                                        Default: $19 <CTRL-Y>
**Mode:** Packet, Command                             Host: CP
**Parameters:**
- "n"  -    0 to $7F (0 to 127 decimal) specifies an ASCII character code.
**Description:**
The parameter "n" is the ASCII code for the character you want to type in order  to cancel an input packet or to cancel display output from the PK-232. You can only cancel the packet that is being entered in CONVERSE Mode.  When you  cancel a packet, the line is terminated with a <BACKSLASH> and a new line.  You  must cancel the packet before typing the send-packet character. In the COMMAND mode, this character cancels displayed output from the PK-232. Typing this character once cancels ALL output from the PK-232 to your display.   Typing the cancel-output character again restores normal output. _______________________________________________________________________________

---

### CASedisp "n"                                           Default: 0 (as is)
**Mode:** Packet                                           Host: CX
**Parameters:**
- "n"  -    0 to 2 specifies how your PK-232 sends characters to your terminal.
**Description:**
CASEDISP allows you to set the case of the characters your PK-232 sends to your  terminal.  CASEDISP offers three possible modes: CASEDISP 0     "As is" - characters are not changed. CASEDISP 1     "lower" - all characters are displayed in lower case only. CASEDISP 2     "UPPER" - all characters are displayed in upper case only. CASEDISP has no effect on your transmitted data.

_______________________________________________________________________________

---

### CBell ON|OFF                                                Default: OFF
**Mode:** Packet/AMTOR                                          Host: CU
**Parameters:**
- ON   -    Three BELL characters <CTRL-G> ($07) are sent to your terminal with 
- the "*** CONNECTED to or DISCONNECTED from (call sign)" message.
- OFF  -    BELLS are NOT sent with the CONNECTED or DISCONNECTED message.
**Description:**
Set CBELL ON if you want to be notified when someone connects to or disconnects  from your station in Packet, or upon establishing a link in AMTOR. _______________________________________________________________________________

---

### CFrom all,none,yes/no call1[,call2..]                       Default: all
**Mode:** Packet                                                Host: CF
**Parameters:**
- call   -  all, none, YES list, NO list.
- List of up to 8 call signs, separated by commas.
**Description:**
CFROM determines how your PK-232 responds to connect requests from other  stations.  CFROM is set to "all" when you first start your PK-232. To reject all call requests, type CFROM NONE.  Your PK-232 sends the calling  station a DM packet, or "busy signal." To accept calls from one or more specific stations, type CFROM YES (followed by  a list of calls signs).  Connects will be accepted from stations whose call  signs are listed after CFROM YES.  For example: cmd:cfrom yes WX1AAA,WX2BBB,WX3CCC,WX4DDD To reject calls from one or more specific stations, type CFROM NO (followed by a  list of call signs).  Connect requests will be ignored from stations whose call  signs are listed after CFROM NO. You can include optional SSIDs specified as "-n" after the call sign.  If CFROM  is set to "no W2JUP", connect attempts from all SSIDs of W2JUP (W2JUP-0 through  W2JUP-15) will be ignored.  If CFROM is set to "yes W2JUP-1", then only W2JUP-1  will be allowed to connect.  Clear CFROM with "%" "&" or "OFF" as arguments. _______________________________________________________________________________

---

### CHCall ON|OFF                                               Default:  OFF
**Mode:** Packet                                                Host: CB
**Parameters:**
- ON   -    Call sign of the distant station IS displayed in multiple connection 
- packet operation.
- OFF  -    Call sign of the distant station is NOT displayed (default).
**Description:**
With CHCALL ON, the call signs of the distant stations appear after the channel  identifier when you are connected to more than one packet station.  When CHCALL  is OFF, only the channel number is displayed in multiple connection operation.

When CHCALL is OFF, the monitored activity looks like this: :0hi John hello Mike how goes it? :1*** CONNECTED to N7GMF :1must be a dx record. ge John When CHCALL is ON, the same contact has the additional underlined information: :0:N7ML:hi John hello Mike how goes it? :1:N7GMF:*** CONNECTED to N7GMF :1must be a dx record. ge John _______________________________________________________________________________

---

### CHDouble ON|OFF                                             Default: OFF
**Mode:** Packet                                                Host: CD
**Parameters:**
- ON   -    Received CHSWITCH characters appear twice (doubled).
- OFF  -    Received CHSWITCH characters appear once (not doubled).
**Description:**
CHDOUBLE ON displays received CHSWITCH characters as doubled characters. Set CHDOUBLE ON When operating with multiple connections to tell the difference  between CHSWITCH characters received from other stations and CHSWITCH characters  generated by your PK-232.  In the following example CHDOUBLE is ON and CHSWITCH  is set to "|" ($7C): || this is a test. The sending station actually transmitted: | this is a test. The same frame received with CHDOUBLE OFF would be displayed as: | this is a test. _______________________________________________________________________________

---

### CHeck "n"                                         Default: 30 (300 sec.)
**Mode:** Packet                                      Host: CK
**Parameters:**
- "n"  -    0 to 250 specifies the check time in ten-second intervals.
- 0    -    Zero disables this feature.
**Description:**
CHECK sets a time-out value for a packet connection if the distant station has  not been heard from for CHECK times 10 seconds. Without the CHECK feature, if your PK-232 were connected to another station and  the other station disappeared, your PK-232 would remain connected indefinitely,  perhaps refusing connections from other stations.

Your PK-232 tries to prevent this sort of "lockup" from occurring depending on  the settings of AX25L2V2 and RECONNECT, by using the CHECK timer as follows: o    If a Version 1 link is inactive for (CHECK times 10 seconds), your PK-232  tries to save the link by starting a reconnect sequence.  The PK-232 enters  the "connect in progress" state and sends "connect request" frames.  o    If a Version 2 link (AX25L2V2 ON) is inactive and packets have not been  heard from the distant end for "n" times 10 seconds, your PK-232 sends a  "check packet" to test if the link still exists to the other station.  If  your PK-232 does not get an answer to the "check packet" after RETRY+1  attempts, it will attempt to reconnect to the distant station. See the RELINK command. _______________________________________________________________________________

---

### CHSwitch "n"                                                Default:  $00
**Mode:** Packet                                                Host: CH
**Parameters:**
- "n"  -    0 to $FF (0 to 255 decimal) specifies an ASCII character code.
**Description:**
CHSWITCH selects the character used by both the PK-232 and the user to show that  a new connection channel is being addressed.  DO NOT USE $30 to $39 (0 to 9). If you plan to use multiple packet connections, you MUST select a CHannel  SWITCHing character.  This character will be interpreted by the PK-232 to  indicate that you want to select another "logical" packet channel. The vertical bar "|" ($7C) is not used often in conversations and makes a good  switching character.  To make the Channel Switching character the vertical bar,  simply enter the command CHSWITCH $7C. To change the logical packet channel you are using with the PK-232, then simply  type the vertical bar "|" followed by a number 0 through 9 indicating which  logical channel you wish the PK-232 to use. See CHDOUBLE and CHCALL for further information on the use of CHSWITCH. _______________________________________________________________________________

---

### CMdtime "n"                                       Default: 10 (1000 msec.)
**Mode:** All                                         Host: CQ
**Parameters:**
- "n"  -    0 to 250  specifies TRANSPARENT Mode time-out value in 100-millisecond 
- intervals.
- If "n" is 0 (zero), exit from Transparent Mode requires sending the 
- BREAK signal or interruption of power to the PK-232.
**Description:**
CMDTIME sets the time-out value in Transparent Mode.  A guard time of "n" times  10 seconds allows escape to Command Mode from Transparent Mode, while permitting  any character to be sent as data. The same Command Mode entry character COMMAND (default <CTRL-C>) is used to exit Transparent Mode, although the procedure is different than from Converse mode. Three Command Mode entry characters must be entered lose than On'. times 10 seconds apart, with no intervening characters, after a delay of 'n' times 10 seconds following the last characters typed. The following diagram illustrates this timing: see manual _______________________________________________________________________________

---

### CKSG ON OFF Default: OFF
**Mode:** Packet Host: CM
**Parameters:**
- ON   -   The recorded CTEXT message is sent as the first packet after a
- connection is established by a connect request from a distant station.
- OFF -  The text message is not sent at all.
**Description:**
CMSG (default OFF) enables or disables automatic transmission of the CTEXT message when your PK-232 accepts a connect request from another station.  Set CMSG ON to give others a message when they connect to your PK-232 or invite them to leave a message on your MailDrop if you are not there. (See MTEXT.)

_______________________________________________________________________________

---

### COMmand "n" Default: 03 <CTRL-C>
**Mode:** All Host: CM
**Parameters:**
- "n"        0 to $7F (O to 127 decimal) specifies an ASCII character code.
**Description:**
COMMAND changes the Command Mode entry character (default <CTRL-C>).  Type the COMMAND character to enter Command Made from the Converse or Transparent Mode. The Command prompt (cmd:) appears, indicating successful entry to Command Mode. See the CKDTIME command. _______________________________________________________________________________

---

### CONmode CONVERSE/TRANS Default: CONVERSE
**Mode:** Packet, Pactor Host: CB
**Parameters:**
- CONVERSE- Your PK-232 enters Converse Mode when a connection in established.
- TRANS - Your PK-232 enters Transparent when a connection is established.
**Description:**
CONMODE selects the mode your PK-232 uses after entering the CONNECTED state. For most operation, setting CONMODE to CONVERS (default) is most natural.

---

### Connect calll (VIA call2(,call3 .... call9) mmediate Command 
**Mode:** Packet Host: CE
**Parameters:**
- calll — Call sign of the distant station to which you wish to be
- connected.
- call2 — Optional call sign(s) of up to eight digipeaters via which
- you'll be
- call9 — repeated to reach the distant station.
- Use the CONNECT command to send a Packet connect request to station “call1,"
- directly or via one or more digipeaters (call2 through call9).  Each call sign
- can include an optional SSID "-n" immediately after the call sign.
- The part of the command line shown in brackets below is optional, and is used
- only when connecting through one or more digipeaters.  Type the digipeater fields
- in the exact sequence you wish to use to route your packets to destination
- station "call1” (Don't type the brackets or quotation marks)
- VIA call2[, ca113 .... ,call9]]
- You can type the command CONNECT at any time to check the status.
- If you are trying to connect to another station, you will see the message;
- Link state is: CONNECT in progress
- If the distant station doesn't "ack" your connect request after the number of
- tries in RETRY, the CONNECT attempt is canceled.  Your monitor displays:
- cmd:*** Retry count exceeded
- DISCONNECTED: (call sign)
**Description:**


---

### CONPerm ON|OFF                                              Default: OFF
**Mode:** Packet                                                Host: CY
**Parameters:**
- ON   -    The connection on the current channel is maintained.
- OFF  -    The current channel can be disconnected from the other stations.
**Description:**
When ON, CONPERM forces the PK-232 to maintain the current connection, even when  frames to the other station exceed RETRY attempts for an acknowledgment.  _______________________________________________________________________________

---

### CONStamp ON|OFF                                             Default: OFF
**Mode:** Packet                                                Host: CG
**Parameters:**
- ON   -    Connect status messages ARE time stamped.
- OFF  -    Connect status messages are NOT time stamped.
**Description:**
CONSTAMP activates time stamping of *** CONNECTED status messages. If CONSTAMP is ON and DAYTIME (the PK-232's internal clock) is set, the time is  sent with CONNECT and DISCONNECT messages.  For example, if the clock is set and  CONSTAMP is ON, a connect and disconnect sequence appears as follows: cmd:10:55:23  *** CONNECTED to W2JUP cmd:10:55:59  *** DISCONNECTED: W2JUP _______________________________________________________________________________

---

### CONVerse                 ( K for short )               Immediate Command
**Mode:** All                                              Host: Not Supported
**Description:**
_______________________________________________________________________________ CONVERSE is an immediate command that causes the PK-232 to switch from the  Command Mode into the Converse Mode.  The letter "K" may also be used. Once the PK-232 is in the Converse Mode, all characters typed from the keyboard  are processed and transmitted by your radio.  To return the PK-232 to the  Command Mode, type the Command Mode entry character (default is <CTRL-C>). _______________________________________________________________________________

---

### CPactime ON|OFF                                             Default: OFF
**Mode:** Packet                                                Host: CI
**Parameters:**
- ON   -    Packet transmit timer IS used in Converse Mode.
- OFF  -    Packet transmit timer is NOT used in Converse Mode.
**Description:**
CPACTIME activates automatic, periodic packet transmission in the Converse Mode. When CPACTIME is ON, characters are packetized and transmitted periodically as  if in Transparent Mode.  Local keyboard editing and display features of the  Converse Mode are available.  See the PACTIME command for a discussion of how  periodic packetizing works.

_______________________________________________________________________________

---

### CRAdd ON|OFF                                                Default: OFF
**Mode:** Baudot RTTY                                           Host: CR
**Parameters:**
- ON   -    Send <CR CR LF> in Baudot RTTY.
- OFF  -    Send <CR LF> in Baudot RTTY (default).
**Description:**
The CRADD command permits you to set the PK-232's "newline" sequence so that an  additional carriage return is ADDed automatically at the end of each typed line. When CRADD is ON the line-end sequence is <CR><CR><LF>.  When CRADD is OFF the  line-end sequence is <CR><LF>.  The double carriage return is required in some  RTTY services such as MARS.  CRADD has no effect on received data. _______________________________________________________________________________

---

### CStatus [Short]                                        Immediate Command
**Mode:** Packet                                           Host: Not Supported
**Description:**
_______________________________________________________________________________ CSTATUS is an immediate command helpful in multiple connections. When CSTATUS is typed, your monitor displays Link State of all ten logical  channels as well as the current input/output channel as follows: NOT CONNECTED TO ANY STATION            CONNECTED TO TWO STATIONS cmd:cs                                  cmd:cs Ch. 0 - IO DISCONNECTED                 Ch. 0 - IO CONNECTED to WX1AAA Ch. 1 -    DISCONNECTED                 Ch. 1 -    CONNECTED to WX1BBB Ch. 2 -    DISCONNECTED                 Ch. 2 -    DISCONNECTED Ch. 3 -    DISCONNECTED                 Ch. 3 -    DISCONNECTED Ch. 4 -    DISCONNECTED                 Ch. 4 -    DISCONNECTED Ch. 5 -    DISCONNECTED                 Ch. 5 -    DISCONNECTED Ch. 6 -    DISCONNECTED                 Ch. 6 -    DISCONNECTED Ch. 7 -    DISCONNECTED                 Ch. 7 -    DISCONNECTED Ch. 8 -    DISCONNECTED                 Ch. 8 -    DISCONNECTED Ch. 9 -    DISCONNECTED                 Ch. 9 -    DISCONNECTED CSTATUS will give a short display if desired.  CSTATUS SHORT (or CS S) displays  only the current input/output channel or those channels which are connected. _______________________________________________________________________________

---

### CText text                                                  Default: empty
**Mode:** Packet                                                Host: CT
**Parameters:**
- text  -   Any combination of characters up to a maximum of 120 characters.
**Description:**
CTEXT is the "automatic answer" text sent when CMSG is ON.  The message is sent  only when another station connects to you.  A typical CTEXT message might be: "I'm not available right now.  Please leave a message on my MailDrop." Clear CTEXT with "%", "&", "NO", "NONE" or "OFF", or simply set CMSG OFF.

_______________________________________________________________________________

---

### CUstom "n"                                                  Default: $0A15
**Mode:** All                                                   Host: Cu
**Parameters:**
- "n"  -    0 to $FFFF (0 to 65,535 decimal) specifies a four digit hexadecimal 
- value, where each bit controls a different function described below.
**Description:**
The CUSTOM command was originally introduced to allow specialized features for  "Custom" applications to be added to the PK-232 without burdening all users with  a plethora of commands.  As the CUSTOM command is quickly filling up, the  command UBIT has been added to replace CUSTOM allow for more features.  The  CUSTOM command is retained for compatibility, but we recommend using the UBIT  command as it is more flexible and easier to use. For those applications that can not take advantage of the UBIT command, the  following CUSTOM features are available in this release of the PK-232 MBX. Bit 0, position $0001:   If bit 0 is set to 1 (default), the PK-232 will discard  a received packet if the signal is too weak to light  the DCD LED.  If set to 0, packets will be received  regardless of the Threshold control setting. Bit 1, position $0002:   If bit 1 is set to 0 (default), then setting the  MONITOR command to either ON or YES will result in a  MONITOR value of 4.  If bit 1 is set to 1, then MONITOR  ON or YES will force the MONITOR value to 6. Bit 2, position $0004:   If bit 2 is set to 1 (default), a break on the RS-232  line will put the PK-232 into the Command mode  (except from Host Mode).  If set to 0, a break on the  RS-232 line will not affect the PK-232. Bit 3, position $0008:   If bit 3 is set to 0 (default), packet channel numbers  will be numbered from 0-9.  If bit 3 is set to one,  then packet channel numbers are labeled A-J or a-j. Bit 4, position $0010:   This bit affects only Baudot transmit.  If bit 4 is set  to 1 (default), the PK-232 inserts the FIGS character  after a space, just prior to sending any figures,  <space><FIGS><number>.  This permits any receiving  station to correctly decode groups of figures  regardless of the USOS setting.  If bit 4 is set to 0,  the PK-232 will NOT insert FIGS characters after each  space.  MARS operators may want to set this bit to 0  for literal operation. Bit 5, position $0020:   If bit 5 is set to 0, (default) the PK-232 will always  power up in the Command mode.  If bit 5 is set to 1,  then the PK-232 will remain in the previous mode, i.e.,  converse, command, or transparent Mode. Bit 6, position $0040:   If bit 6 is set to 0, (default), then monitoring is  disabled in the Transparent mode.  If bit 6 is set to  1, then monitoring is active in the transparent mode.   MFROM, MTO, MRPT, MONITOR, MCON, MPROTO, MSTAMP,  CONSTAMP, and MBX are all active.

Bit 7, position $0080:   If bit 7 is set to 0 (default), the PK-232 prints  the ..-- Morse character as ^.  If bit 7 is set to 1,  the PK-232 decodes ..-- as a carriage return. Bit 8, position $0100:   If bit 8 is set to 0 (default), MORSE will configure  the PK-232 filters for CW as before.  If bit 8 is set  to 1, MORSE configures the filters for FSK (two-tone)  operation, in both receive and transmit.  WIDESHFT,  RXREV and TXREV are active. Bit 9, position $0200:   Bit position 9 does for AMTOR what WRU does for Baudot  and ASCII.  If bit 9 is set to 1 (default), a FIGS-D in  ARQ causes the unit to become the ISS, transmit the AAB  string, then revert back to the IRS.  If bit 9 is set  to 0, then a received FIGS-D will have no effect. Bit 10, position $0400:  If bit 10 is set to 0 (default), then host polling is  as before.  If bit 10 is set to 1, then any change in  status (e.g. Idle to Tfc) in AMTOR, FAX, TDM or NAVTEX  causes the PK-232 to issue the following host block: SOH  $50  n  ETB where n is $30-36, the same number that the OPMODE  command furnishes.  This block is subject to HPOLL. Bit 11, position $0800:  If bit 11 is set to 1 (default), a connected message  appears when an ARQ link is first established using  seven-character SELCALLs (CCIR 625).  If bit 11 is set  to 0, no connected message appears at the beginning of  ARQ communications. Bit 12, position $1000:  If bit 12 is set to 0 (default), the Packet Morse ID  (MID) is on/off keying of the low tone.  If bit 12 is  set to 1, the Morse ID is sent in 2-tone FSK with the  low tone being key-down and the high tone representing  key-up.  Use this setting to keep other stations from  sending a packet during the Morse ID. Bit 13, position $2000:  If bit 13 is set to 0 (default), MailDrop connect  status messages are always sent to the local user,  regardless of the setting of MDMON.  If bit 13 is set  to 1, remote user dialog and connect status messages  with the MailDrop are shown only if MDMON is ON. Bit 14, position $4000:  If bit 14 is set to 0 (default), the transmit buffer  for data sent from the computer to the PK-232 in packet  mode is limited only by available PK-232 memory.  If  bit 14 is set to 1, the serial flow control will permit  only a maximum of 7 I-frames to be held by the PK-232  before transmission.  This solves a problem with the  YAPP binary file transfer program which relies on a  small TNC transmit buffer to operate correctly. Bit 15 is unused at the present time.  To return CUSTOM to the default setting,  type CU Y or CU ON at the command prompt.

_______________________________________________________________________________

---

### CWid "n"                                          Default: $06 <CTRL-F>
**Mode:** Baudot/ASCII RTTY/AMTOR/FAX                 Host: CW
**Parameters:**
- The CWID command lets you change the "send CWID" control character typed at the 
- end of your Baudot and ASCII RTTY dialogue.
- When the PK-232 reads this character embedded in the text or keyboard input, it 
- switches modes and sends your call sign in Morse code, at the keying speed set 
- by MSPEED.  As soon as your call sign has been sent in Morse, the PK-232 turns 
- off your transmitter and returns to receive.
**Description:**


---

### DAYStamp ON|OFF                                             Default: OFF
**Mode:** All                                                   Host: DS
**Parameters:**
- ON   -    The DATE is included in CONSTAMP and MSTAMP.
- OFF  -    Only the TIME is included in CONSTAMP and MSTAMP.
**Description:**
DAYSTAMP activates the date in CONSTAMP and MSTAMP.  Set DAYSTAMP ON when you  want a dated record of packet channel activity, or when you're unavailable for  local packet operation. _______________________________________________________________________________

---

### DAytime date & time                                         Default: none
**Mode:** All                                                   Host: DA
**Parameters:**
- date & time -  Current DATE and TIME to set.
**Description:**
DAYTIME sets the PK-232's internal clock current date and time.  The date & time  is used in many modes and should be set when the PK-232 is powered up. The clock is not set when the PK-232 is turned on.  The DAYTIME command displays  the "?clock not set" error message until it is set as follows: yymmddhhmm[ss]         (spaces and punctuation are allowed) Example:  cmd:daytime 9003090659 where: yy is the last two digits of the year mm is the two-digit month code (01-12) dd is date (01-31) hh is the hour (00-23) mm is the minutes after the hour (00-59) [ss] is the optional seconds o    Optionally the Dallas Semiconductor DS-1216C SmartWatch may be added to the  PK-232.  To install this IC carefully remove the 32K RAM IC and install the  SmartWatch in the RAM socket.  Then re-install the RAM IC in the socket  provided by the SmartWatch.

_______________________________________________________________________________

---

### DCdconn ON|OFF                                              Default: OFF
**Mode:** Packet/AMTOR KISS and RAWHDLC                         Host: DC
**Parameters:**
- ON   -    RS-232 cable Pin 8 follows the state of the CON (or DCD) LED.
- OFF  -    RS-232 cable Pin 8 is permanently set high (default).
**Description:**
DCDCONN defines how the DCD (Data Carrier Detect) signal affects pin 8 in the  RS-232 interface to your computer or terminal.  Some programs such as PBBS  software require that DCDCONN be ON. DCDCONN also works in the RAWHDLC and KISS Modes.  In RAWHDLC and KISS Modes, no  packet connections are known to the PK-232.  When DCDCONN is ON, the state of  the radio DCD is sent to the RS-232 DCD pin (pin-8).  This may be necessary to  some host applications that need to know when the radio channel is busy. _______________________________________________________________________________

---

### DELete ON|OFF                                               Default: OFF
**Mode:** All                                                   Host: DL
**Parameters:**
- ON   -    The <DELETE> ($7F) key is used for editing your typing.
- OFF  -    The <BACKSPACE> ($08) key is used for editing your typing.
**Description:**
Use the DELETE command to select the key to use for deleting while editing. Set DELETE OFF (default) if you wish to use the <Backspace> key to edit typing  mistakes.  Set DELETE ON if you wish to use the <Delete> key to edit mistakes. See the BKONDEL command controls how the PK-232 indicates deletion.

_______________________________________________________________________________

---

### DFrom all,none,yes/no call1[,call2..]                       Default: all
**Mode:** Packet                                                Host: DF
**Parameters:**
- call   -  all, none, YES list, NO list.
- list of up to eight call signs, separated by commas.
**Description:**
DFROM determines how your PK-232 responds to stations trying to use your station  as a digipeater.  DFROM is set to "all" when you first start your PK-232. Type DFROM to display the ALL/NONE/YES_list/NO_list status of station's call  signs whose packets will or will not be repeated. To prevent all stations from digipeating through your station, type DFROM NONE. To permit one or more specific stations to digipeat through your station, type  DFROM YES (followed by a list of calls signs).  Packets will be digipeated only  from stations whose call signs are listed. To prevent one or more specific stations to digipeat through your station, type  DFROM NO (followed by a list of call signs).  Packets will not be digipeated  from stations whose call signs are listed. Clear DFROM with "%" "&" or "OFF" as arguments. _______________________________________________________________________________

---

### DIDdle: ON|OFF                                              Default: OFF
**Mode:** Baudot, ASCII                                         Host: DD
**Parameters:**
- ON    -   In Baudot, the PK-232 will send the LTRS character when idling.
- In ASCII, the PK-232 sends the NULL (00) character.
- OFF   -   No characters are sent when idling in transmit.
**Description:**
In the RTTY modes, it may be desirable to continue sending some data while  paused at the keyboard.  With DIDDLE on, the PK-232 will send idle characters  when waiting for keyboard entry. ________________________________________________________________________________

---

### Disconne                                               Immediate Command
**Mode:** Packet                                           Host: DI
**Description:**
_______________________________________________________________________________ DISCONNE is an immediate command that initiates a disconnect command to the  distant station to which you are connected.  If your disconnect command is  successful, your monitor will display: *** DISCONNECTED: (call sign) Other commands can be entered while a disconnect is in progress. New connections are not allowed until the disconnect is completed. o    If another disconnect command is entered while your PK-232 is trying to  disconnect, your PK-232 will instantly switch to the disconnected state.

_______________________________________________________________________________

---

### DISPlay [class]                                        Immediate Command
**Mode:** Command                                          Host: Not Supported
**Parameters:**
- class -   Optional parameter identifier, one of the following: 
- (A)sync       display asynchronous port parameters 
- (B)BS         display AMTOR and Packet MailDrop parameters
- (C)haracter   display special characters 
- (F)ax         display Facsimile parameters
- (I)d          display ID parameters 
- (L)ink        display link parameters 
- (M)onitor     display monitor parameters 
- (R)TTY        display RTTY parameters
- (T)iming      display timing parameters 
- (Z)           display the entire command/parameter list 
**Description:**
DISPLAY is an immediate command.  When DISPLAY is typed without a parameter, the  PK-232 responds with a short list of often used parameters. (See also DISPLAY A,B,C,F,I,L,M,R,T,Z) Connect   Link state is: DISCONNECTED Opmode    PAcket     FRack     5 (5 sec.) HBaud     1200 MAXframe  4 Monitor   4 (UA DM C D I UI) MYcall    PK232 MYSelcal  none PACLen    128 RBaud     45 TXdelay   30 (300 msec.) Vhf       ON WIdeshft  OFF You can display subgroups of related system parameters by specifying the  optional class parameter.  For example, to display the AMTOR and Packet MailDrop  parameters type: disp b 3Rdparty  OFF FREe      18340 KILONFWD  ON LAstmsg   0 MAildrop  OFF MDMon     OFF MDPrompt  Subject:/Enter message, ^Z (CTRL-Z) or /EX to end MMsg      OFF MTExt     Welcome to my AEA PK-232M maildrop. Type H for help. MYMail    none TMail     OFF TMPrompt  GA subj/GA msg, '/EX' to end. Command names are shown with UPPER-CASE letters indicating the minimum number of  characters required for the command.  The lower-case letters indicate the  (optional) rest of the command name.

cmd:disp b 3Rdparty  OFF FREe      18480 KILONFWD  ON MAildrop  OFF MDMon     OFF MDPrompt  Subject:/Enter message, ^Z (CTRL-Z) or /EX to end MMsg      OFF MTExt     Welcome to my AEA PK-232M maildrop. Type H for help. MYMail    none Command names are shown with UPPER-CASE letters indicating the minimum number of  characters required for the command.  The lower-case letters indicate the  (optional) rest of the command name. _______________________________________________________________________________

---

### DWait "n"                                         Default: 16 (160 msec.)
**Mode:** Packet                                      Host: DW
**Parameters:**
- "n"  -    0 to 250 specifies wait time in ten-millisecond intervals.
**Description:**
Unless the PK-232 is waiting to transmit digipeated packets, DWAIT forces your  PK-232 to pause DWAIT x 10 mSec after last hearing data on the channel, before  it begins its transmitter keyup sequence. DWAIT is an older way collisions with digipeated packets were avoided.  These  days the P-PERSISTENT method is generally used.  When the PPERSIST command is ON  (default) the DWAIT timer is ignored. _______________________________________________________________________________

---

### EAS ON|OFF                                                  Default: OFF
**Mode:** Baudot, ASCII, AMTOR and MORSE                        Host: EA
**Parameters:**
- ON    -   Echo characters when actually sent on the air by the PK-232.
- OFF   -   Echo characters when sent to the PK-232 by the computer.
**Description:**
The ECHO-AS-SENT (EAS) command functions in all modes except packet.  EAS  lets you to choose the way data is displayed on your monitor screen or printer. To display your typing exactly as you are typing the keyboard characters or  sending from a disk file, set EAS "OFF" (default).  To see the actual data being  sent from your PK-232 to your radio and transmitted on the air, set EAS "ON". When EAS is ON in Morse and Baudot RTTY, you'll see only UPPER CASE characters  on your screen - the data actually transmitted to the distant station. If EAS is ON in AMTOR Mode A (ARQ), you'll see characters echoed on your screen  only after the distant station has validated (Ack'd) your block of three  characters.  For Packet the MXMIT command should be used. Nulls ($00) are not echoed, including the nulls produced by DIDDLE ON in ASCII.

_______________________________________________________________________________

---

### Echo ON|OFF                                                 Default: ON
**Mode:** All                                                   Host: EC
**Parameters:**
- ON   -    Characters received from the terminal ARE echoed by the PK-232.
- OFF  -    Characters are NOT echoed.
**Description:**
The ECHO command controls local echoing by the PK-232 when in Command or  Converse Mode.  Local echoing is disabled in Transparent Mode. o    Set ECHO ON (default) if you don't see your typing appear on your display. o    Set ECHO OFF if you see each character you type doubled. ECHO is set correctly when you see the characters you type displayed correctly. _______________________________________________________________________________

---

### ERrchar "n"                                            Default: $5F (_)
**Mode:** AMTOR, Morse, NAVTEX and TDM                     Host: ER
**Parameters:**
- "n"   -   A hexadecimal value from $00-$7F used to denote the error character 
- used by the PK-232 for Morse, ARQ, FEC, NAVTEX and TDM.
**Description:**
n is a hex value $00-7F, default $5F (underscore).  This is the character that  the PK-232 displays when it receives a mutilated character in Morse, ARQ,  FEC, NAVTEX or TDM.  The user may wish to set this character to $2A (asterisk),  $07 (bell), $20 (space) or $00 (null).  ERRCHAR ON or ER Y restores the default. _______________________________________________________________________________

---

### EScape ON|OFF                                               Default: OFF
**Mode:** All                                                   Host: ES
**Parameters:**
- ON   -    The <ESCAPE> character ($1B) is output as "$" ($24).
- OFF  -    The <ESCAPE> character is output as <ESCAPE> ($1B) (default).
**Description:**
The ESCAPE command selects the character to be output when an <ESCAPE> character  is to be sent to the terminal.  The ESCAPE character selection is provided  because some computers and terminals interpret the <ESCAPE> character as a  special command.  Set ESCAPE ON if you have an <ESCAPE> sensitive terminal to  avoid unexpected results from accidentally receiving this character. _______________________________________________________________________________

---

### FAx                                                    Immediate Command
**Mode:** Command                                          Host: FA
**Description:**
_______________________________________________________________________________ FAX is an immediate command that switches your PK-232 into the facsimile mode.   The FAX mode is available only if the maildrop (or FREE command) shows at least  3742 bytes free.  You must kill MailDrop messages until the number reaches this  level if you wish to operate FAX.

_______________________________________________________________________________

---

### FAXNeg  ON|OFF                                              Default: OFF
**Mode:**  FAX                                                  Host: FN
**Parameters:**
- ON   -    The white and black senses are reversed
- OFF  -    The white and black senses are normal
**Description:**
One might use FAXNEG ON when receiving an image consisting mostly of black, as  in a satellite photo.  In this case it might help to save your printer ribbon,  as well as accentuating features such as cloud cover. FAXNEG ON is NOT the same as RXREV ON.  RXREV reverses the entire signal,  including the sync pulses.  FAXNEG keeps the sync pulses so that they can be  recognized, but reverses the image data. _______________________________________________________________________________

---

### FEc                                                    Immediate Command
**Mode:** AMTOR Mode B                                     Host: FE
**Description:**
_______________________________________________________________________________ FEC is an immediate command that starts an AMTOR FEC (Mode B) transmission. Use FEC for CQ calls in AMTOR.  Be sure to include your SELCALL and MYIDENT code  in your CQ message so that the distant station can call you back in ARQ. FEC is necessary for all round table AMTOR contacts.  When operating in FEC, let  your PK-232 begin each transmission with three to five seconds of idling.  The  RTTY practice of transmitting a line of RYRYRY is unnecessary on FEC. You can signify the end of your FEC transmission by typing the changeover sign  "+?," internationally recognized as the RTTY equivalent of "KKK."  However, in  FEC, "+?" is not a software command.  You still have to un-key your transmitter  (with the RECEIVE or CWID characters or the RCVE command) as you would in RTTY. _______________________________________________________________________________

---

### Flow ON|OFF                                                 Default: ON
**Mode:** All                                                   Host: FL
**Parameters:**
- ON   -    Type-in flow control IS active.
- OFF  -    Type-in flow control is NOT active.
**Description:**
When FLOW is ON (default), any character typed on your keyboard causes output  from the PK-232 to the terminal to stop until any of the following occurs: o    A packet is sent (in Converse Mode) o    A line is completed (in Command Mode) o    The packet length (See PACLEN) is exceeded o    The current packet or command line is canceled o    The redisplay-line character is typed o    The logical packet channel is changed Setting FLOW ON prevents received data from interfering with your keyboard data  entry.  When FLOW is OFF, data is sent to the terminal whenever it is available.

---

### FRack  “In” Default: 5 (5 sec.)
**Mode:** Packet Host: FR
**Parameters:**
- “n” — 1 to 15, specifying FRame ACKnowledgment timeout in 1 second
- intervals.
- FRACK is the FRame Acknowledgment time in seconds that your PK-232 will wait for
- acknowledgment of a sent protocol frame before "retrying" that frame.
- After sending a packet requiring acknowledgment, the PK-232 waits for FRACK
- seconds before incrementing the retry counter and sending another frame.  If the
- packet address includes any digipeaters, the time between retries is adjusted to:
- Retry interval (in seconds) = "n" x (2 x m + 1)
- (where m is the number of intermediate relay stations.)
- When a packet is retried, a random wait time is added to any other wait times.
- This avoids lockups where two packet stations repeatedly collide with each other.
- FREe Immediate Command
- Mode: All Host: FZ
- Typing "FREE" displays the number of usable bytes left in the MailDrop, as in
- "FREE 3724." This may be useful to a Host mode application using the MailDrop.
- FRIck "n/n” Default: 0/0 (0 sec.)
- Mode: Packet Host: FF
- Parameters:
- “n”        —   0 to 250, specifying the Frame Acknowledgment timeout for
- Meteor Scatter work in 10 milli-second intervals.
- FRICK is a short version of FRACK, meant to be used in packet radio meteor
- scatter work.  If FRICK is 0 (default), the FRACK timer is then in use and the
- unit operates as before with the retry timer in units of whole seconds.  If FRICK
- is 1 to 250, FRICK overrides FRACK as the unit's retry timer, and the retry timer
- is in units of 10 msec. up to 2500 mode. (2.5 seconds).
- Unlike FRACK, FRICK does not take into account the number of digipeaters in the
- connect path.  FRICK assumes there are no digipeaters being used.
- Note: Do not attempt multiple packet connections while FRICK is active (1-250).
- In contrast to FRACK, which provides one retry timer per multi-connect channel,
- there is only one FRICK timer in the PK-232.  Each logical channel will try to
- use the same FRICK timer, causing interference to the operation of the other
- channels.
- Due to the sporadic nature of meteor scatter work, a Master/Slave mode can be
- enabled in the PK-232 with User BIT 18 (UBIT 18).  When UBIT IS OFF, Frame
- Acknowledge operation is as in previous firmware versions.
- When UBIT 18 is ON, a master/slave relationship is established in packet radio
- connections.  This is done to reduce the possibility of simultaneous
- transmissions by both sides of a packet connection.  In this mode, the master
- station sends either an I-Frame or a polling frame upon the expiration of FRICK
- (or FRACK if FRICK = 0).  The FRICK or FRACK timer then starts counting again.
- The master station therefore sends packets constantly, even if all its I-frames
- have been acknowledged.  The slave station sends nothing, not even I-frames,
- until it receives a polling frame from the master.  A station becomes the master
- upon its transmission of a SABM (connect) frame; a station becomes the slave upon
- its transmission of a UA (acknowledgement of the SABM) frame.
- Recommended settings for this method of meteor scatter work (both stations should
- use these settings):
- UBIT IS ON
- RETRY 0
- AX25L2V2 ON (default)
- MLXFRAME 1
- (CHECK doesn't matter)
- FRICK n, where n is large enough to allow the other station time to
- send the start of an acknowledgement frame
- Note; This is an experimental mode and we welcome any comments or suggestions you
- might have.  Please make them in writing and direct them to the Timewave
- Engineering Department.  Thank You.
- FSpeed "n" Default: 2 (120)
- Mode: FAX Host: FS
- Parameters:
- “n”        —   0 to 4 selects the FAX horizontal scan rate from the table below:
- 1: 1 line/Second  60 lines/Minute
- 2:  2 lines/Second 120 lines/Minute
- 3: 3 lines/Second 180 lines/Minute
- 4: 4 lines/Second 240 lines/Minute
- 0: 1.5 lines/Second  90 lines/Minute
- You can tell the scan rate by listening to the signal.  Most weather charts are
- transmitted at 2 lines/Second (default), or 120 lines/Minute.  Some facsimile
- photographs and Japanese news are sent at 60 lines/minute.
- With wide-carriage printers, the maximum print densities are reduced.  Here are
- the maximum print densities at various scan speeds and carriage widths:
- FSPEED     (LPM)         Standard carriage Wide carriage
- 0 90 183 dpi 113 dpi
- 1 60 275 dpi 169 dpi
- 2 120 138 dpi  85 dpi
- 3 150  92 dpi  56 dpi
- 4 240  69 dpi  42 dpi
- FUlldup ON/OFF Default: OFF
- Mode: Packet Host: FU
- Parameters:
- ON  -  Full duplex mode is ENABLED.  
- OFF - Full duplex mode is DISABLED.
- When full-duplex mode is OFF (default), the PX-232 makes use of the DCD (Data
- Carrier Detect) signal from its modem to avoid collisions.
- When full-duplex mode is ON the PK-232 ignores the DCD signal and acknowledges
- packets individually.
- Full-duplex operation is useful for full-duplex radio operation, such as through
- OSCAR satellites.  It should not be used unless both your station and the distant
- station can operate in full-duplex.
- Graphics  “n” Default: 1 (960 dots)
- Mode: FAX Host: GR
- Parameters:
- “n" 0 to 6 selects the FAX horizontal graphics dot density printed on the
- printer from the table below
- GRAPHICS determines the horizontal print density of the parallel Printer.  The
- GRAPHICS dot densities for each PRTYPE will be given with the PRTYPE command.
- Graphics dot-densities as a function of PRTYPE are shown below.
- Density in dote/inch (dpi) as a function of GRAPHICS and
- PRTYPE
- GRAPHICS
- PRTYPE    0        1        2        3        4        5        6
- 0-3 60 120 120 240 50 72 90
- 4-7 60 120 120 240 50 72 90
- 8-9 60 120 144 200 80 72 90
- 12-19 136 240 144 160 50 72 96
- 20-21 60 60 60 60 60 72 100
- 24-27 60 120 144 240 60 72
- 28-29 60 120
- 32-35 60 120 120 240 80 72 90
- 36 60 60 60 60 60 60 60
- 40-43 60 120 120 120 60 72 144
- 44-47 72 144 144 72 72 72 72
- 48-51 50 160 80 50 50 80 80
- In using the various GRAPHICS densities above, the user should be aware that not
- all the combinations or parameters work, especially with the slower printers (100
- CPS or less).  For example, a combination of PRTYPE 2, FSPEED 4, GP.APHICS I and
- ASPECT 4 would require the printer to print a pattern of 8 dots by 960 every 3
- seconds which would mean trouble for a 100 CPS printer.  On the other hand, a
- combination of PRTYPE 2, FSPEED 2, GRAPHICS 0 and ASPECT 2 would work, as it
- results in a pattern of dots 8 by 480 every 12 seconds.  We know the following
- combinations of dot densities and FSPEED cause trouble.
- FSPEED, 8" width (narrow)      FSPEED 13" width (wide)
- Dot Density         0    1    2    3    4         0    1    2    3    4
- 60   dpi
- 72   dpi                                                              s
- 80   dpi                                                              x
- 90   dpi                                                              x
- 96   dpi                                                         s    x
- 100  dpi                                                         s    x
- 120  dpi                                s                        x    x
- 136  dpi                                x                        x    x
- 144  dpi                                x                   s    x    x
- 160  dpi                           s    x         x    x    x    x    x
- 200  dpi                           x    x         x    x    x    x    x
- 240  dpi                      s    x    x         x    x    x    x    x
**Description:**


---

### FREe Immediate Command
**Mode:** All Host: FZ
**Description:**
Typing "FREE" displays the number of usable bytes left in the MailDrop, as in "FREE 3724." This may be useful to a Host mode application using the MailDrop.

---

### FRIck "n/n” Default: 0/0 (0 sec.)
**Mode:** Packet Host: FF
**Parameters:**
- “n”        —   0 to 250, specifying the Frame Acknowledgment timeout for
- Meteor Scatter work in 10 milli-second intervals.
- FRICK is a short version of FRACK, meant to be used in packet radio meteor
- scatter work.  If FRICK is 0 (default), the FRACK timer is then in use and the
- unit operates as before with the retry timer in units of whole seconds.  If FRICK
- is 1 to 250, FRICK overrides FRACK as the unit's retry timer, and the retry timer
- is in units of 10 msec. up to 2500 mode. (2.5 seconds).
- Unlike FRACK, FRICK does not take into account the number of digipeaters in the
- connect path.  FRICK assumes there are no digipeaters being used.
- Note: Do not attempt multiple packet connections while FRICK is active (1-250).
- In contrast to FRACK, which provides one retry timer per multi-connect channel,
- there is only one FRICK timer in the PK-232.  Each logical channel will try to
- use the same FRICK timer, causing interference to the operation of the other
- channels.
- Due to the sporadic nature of meteor scatter work, a Master/Slave mode can be
- enabled in the PK-232 with User BIT 18 (UBIT 18).  When UBIT IS OFF, Frame
- Acknowledge operation is as in previous firmware versions.
- When UBIT 18 is ON, a master/slave relationship is established in packet radio
- connections.  This is done to reduce the possibility of simultaneous
- transmissions by both sides of a packet connection.  In this mode, the master
- station sends either an I-Frame or a polling frame upon the expiration of FRICK
- (or FRACK if FRICK = 0).  The FRICK or FRACK timer then starts counting again.
- The master station therefore sends packets constantly, even if all its I-frames
- have been acknowledged.  The slave station sends nothing, not even I-frames,
- until it receives a polling frame from the master.  A station becomes the master
- upon its transmission of a SABM (connect) frame; a station becomes the slave upon
- its transmission of a UA (acknowledgement of the SABM) frame.
- Recommended settings for this method of meteor scatter work (both stations should
- use these settings):
- UBIT IS ON
- RETRY 0
- AX25L2V2 ON (default)
- MLXFRAME 1
- (CHECK doesn't matter)
- FRICK n, where n is large enough to allow the other station time to
- send the start of an acknowledgement frame
- Note; This is an experimental mode and we welcome any comments or suggestions you
- might have.  Please make them in writing and direct them to the Timewave
- Engineering Department.  Thank You.
- FSpeed "n" Default: 2 (120)
- Mode: FAX Host: FS
- Parameters:
- “n”        —   0 to 4 selects the FAX horizontal scan rate from the table below:
- 1: 1 line/Second  60 lines/Minute
- 2:  2 lines/Second 120 lines/Minute
- 3: 3 lines/Second 180 lines/Minute
- 4: 4 lines/Second 240 lines/Minute
- 0: 1.5 lines/Second  90 lines/Minute
- You can tell the scan rate by listening to the signal.  Most weather charts are
- transmitted at 2 lines/Second (default), or 120 lines/Minute.  Some facsimile
- photographs and Japanese news are sent at 60 lines/minute.
- With wide-carriage printers, the maximum print densities are reduced.  Here are
- the maximum print densities at various scan speeds and carriage widths:
- FSPEED     (LPM)         Standard carriage Wide carriage
- 0 90 183 dpi 113 dpi
- 1 60 275 dpi 169 dpi
- 2 120 138 dpi  85 dpi
- 3 150  92 dpi  56 dpi
- 4 240  69 dpi  42 dpi
- FUlldup ON/OFF Default: OFF
- Mode: Packet Host: FU
- Parameters:
- ON  -  Full duplex mode is ENABLED.  
- OFF - Full duplex mode is DISABLED.
- When full-duplex mode is OFF (default), the PX-232 makes use of the DCD (Data
- Carrier Detect) signal from its modem to avoid collisions.
- When full-duplex mode is ON the PK-232 ignores the DCD signal and acknowledges
- packets individually.
- Full-duplex operation is useful for full-duplex radio operation, such as through
- OSCAR satellites.  It should not be used unless both your station and the distant
- station can operate in full-duplex.
- Graphics  “n” Default: 1 (960 dots)
- Mode: FAX Host: GR
- Parameters:
- “n" 0 to 6 selects the FAX horizontal graphics dot density printed on the
- printer from the table below
- GRAPHICS determines the horizontal print density of the parallel Printer.  The
- GRAPHICS dot densities for each PRTYPE will be given with the PRTYPE command.
- Graphics dot-densities as a function of PRTYPE are shown below.
- Density in dote/inch (dpi) as a function of GRAPHICS and
- PRTYPE
- GRAPHICS
- PRTYPE    0        1        2        3        4        5        6
- 0-3 60 120 120 240 50 72 90
- 4-7 60 120 120 240 50 72 90
- 8-9 60 120 144 200 80 72 90
- 12-19 136 240 144 160 50 72 96
- 20-21 60 60 60 60 60 72 100
- 24-27 60 120 144 240 60 72
- 28-29 60 120
- 32-35 60 120 120 240 80 72 90
- 36 60 60 60 60 60 60 60
- 40-43 60 120 120 120 60 72 144
- 44-47 72 144 144 72 72 72 72
- 48-51 50 160 80 50 50 80 80
- In using the various GRAPHICS densities above, the user should be aware that not
- all the combinations or parameters work, especially with the slower printers (100
- CPS or less).  For example, a combination of PRTYPE 2, FSPEED 4, GP.APHICS I and
- ASPECT 4 would require the printer to print a pattern of 8 dots by 960 every 3
- seconds which would mean trouble for a 100 CPS printer.  On the other hand, a
- combination of PRTYPE 2, FSPEED 2, GRAPHICS 0 and ASPECT 2 would work, as it
- results in a pattern of dots 8 by 480 every 12 seconds.  We know the following
- combinations of dot densities and FSPEED cause trouble.
- FSPEED, 8" width (narrow)      FSPEED 13" width (wide)
- Dot Density         0    1    2    3    4         0    1    2    3    4
- 60   dpi
- 72   dpi                                                              s
- 80   dpi                                                              x
- 90   dpi                                                              x
- 96   dpi                                                         s    x
- 100  dpi                                                         s    x
- 120  dpi                                s                        x    x
- 136  dpi                                x                        x    x
- 144  dpi                                x                   s    x    x
- 160  dpi                           s    x         x    x    x    x    x
- 200  dpi                           x    x         x    x    x    x    x
- 240  dpi                      s    x    x         x    x    x    x    x
**Description:**


---

### FSpeed "n" Default: 2 (120)
**Mode:** FAX Host: FS
**Parameters:**
- “n”        —   0 to 4 selects the FAX horizontal scan rate from the table below:
- 1: 1 line/Second  60 lines/Minute
- 2:  2 lines/Second 120 lines/Minute
- 3: 3 lines/Second 180 lines/Minute
- 4: 4 lines/Second 240 lines/Minute
- 0: 1.5 lines/Second  90 lines/Minute
- You can tell the scan rate by listening to the signal.  Most weather charts are
- transmitted at 2 lines/Second (default), or 120 lines/Minute.  Some facsimile
- photographs and Japanese news are sent at 60 lines/minute.
- With wide-carriage printers, the maximum print densities are reduced.  Here are
- the maximum print densities at various scan speeds and carriage widths:
- FSPEED     (LPM)         Standard carriage Wide carriage
- 0 90 183 dpi 113 dpi
- 1 60 275 dpi 169 dpi
- 2 120 138 dpi  85 dpi
- 3 150  92 dpi  56 dpi
- 4 240  69 dpi  42 dpi
- FUlldup ON/OFF Default: OFF
- Mode: Packet Host: FU
- Parameters:
- ON  -  Full duplex mode is ENABLED.  
- OFF - Full duplex mode is DISABLED.
- When full-duplex mode is OFF (default), the PX-232 makes use of the DCD (Data
- Carrier Detect) signal from its modem to avoid collisions.
- When full-duplex mode is ON the PK-232 ignores the DCD signal and acknowledges
- packets individually.
- Full-duplex operation is useful for full-duplex radio operation, such as through
- OSCAR satellites.  It should not be used unless both your station and the distant
- station can operate in full-duplex.
- Graphics  “n” Default: 1 (960 dots)
- Mode: FAX Host: GR
- Parameters:
- “n" 0 to 6 selects the FAX horizontal graphics dot density printed on the
- printer from the table below
- GRAPHICS determines the horizontal print density of the parallel Printer.  The
- GRAPHICS dot densities for each PRTYPE will be given with the PRTYPE command.
- Graphics dot-densities as a function of PRTYPE are shown below.
- Density in dote/inch (dpi) as a function of GRAPHICS and
- PRTYPE
- GRAPHICS
- PRTYPE    0        1        2        3        4        5        6
- 0-3 60 120 120 240 50 72 90
- 4-7 60 120 120 240 50 72 90
- 8-9 60 120 144 200 80 72 90
- 12-19 136 240 144 160 50 72 96
- 20-21 60 60 60 60 60 72 100
- 24-27 60 120 144 240 60 72
- 28-29 60 120
- 32-35 60 120 120 240 80 72 90
- 36 60 60 60 60 60 60 60
- 40-43 60 120 120 120 60 72 144
- 44-47 72 144 144 72 72 72 72
- 48-51 50 160 80 50 50 80 80
- In using the various GRAPHICS densities above, the user should be aware that not
- all the combinations or parameters work, especially with the slower printers (100
- CPS or less).  For example, a combination of PRTYPE 2, FSPEED 4, GP.APHICS I and
- ASPECT 4 would require the printer to print a pattern of 8 dots by 960 every 3
- seconds which would mean trouble for a 100 CPS printer.  On the other hand, a
- combination of PRTYPE 2, FSPEED 2, GRAPHICS 0 and ASPECT 2 would work, as it
- results in a pattern of dots 8 by 480 every 12 seconds.  We know the following
- combinations of dot densities and FSPEED cause trouble.
- FSPEED, 8" width (narrow)      FSPEED 13" width (wide)
- Dot Density         0    1    2    3    4         0    1    2    3    4
- 60   dpi
- 72   dpi                                                              s
- 80   dpi                                                              x
- 90   dpi                                                              x
- 96   dpi                                                         s    x
- 100  dpi                                                         s    x
- 120  dpi                                s                        x    x
- 136  dpi                                x                        x    x
- 144  dpi                                x                   s    x    x
- 160  dpi                           s    x         x    x    x    x    x
- 200  dpi                           x    x         x    x    x    x    x
- 240  dpi                      s    x    x         x    x    x    x    x
**Description:**


---

### FUlldup ON/OFF Default: OFF
**Mode:** Packet Host: FU
**Parameters:**
- ON  -  Full duplex mode is ENABLED.  
- OFF - Full duplex mode is DISABLED.
- When full-duplex mode is OFF (default), the PX-232 makes use of the DCD (Data
- Carrier Detect) signal from its modem to avoid collisions.
- When full-duplex mode is ON the PK-232 ignores the DCD signal and acknowledges
- packets individually.
- Full-duplex operation is useful for full-duplex radio operation, such as through
- OSCAR satellites.  It should not be used unless both your station and the distant
- station can operate in full-duplex.
- Graphics  “n” Default: 1 (960 dots)
- Mode: FAX Host: GR
- Parameters:
- “n" 0 to 6 selects the FAX horizontal graphics dot density printed on the
- printer from the table below
- GRAPHICS determines the horizontal print density of the parallel Printer.  The
- GRAPHICS dot densities for each PRTYPE will be given with the PRTYPE command.
- Graphics dot-densities as a function of PRTYPE are shown below.
- Density in dote/inch (dpi) as a function of GRAPHICS and
- PRTYPE
- GRAPHICS
- PRTYPE    0        1        2        3        4        5        6
- 0-3 60 120 120 240 50 72 90
- 4-7 60 120 120 240 50 72 90
- 8-9 60 120 144 200 80 72 90
- 12-19 136 240 144 160 50 72 96
- 20-21 60 60 60 60 60 72 100
- 24-27 60 120 144 240 60 72
- 28-29 60 120
- 32-35 60 120 120 240 80 72 90
- 36 60 60 60 60 60 60 60
- 40-43 60 120 120 120 60 72 144
- 44-47 72 144 144 72 72 72 72
- 48-51 50 160 80 50 50 80 80
- In using the various GRAPHICS densities above, the user should be aware that not
- all the combinations or parameters work, especially with the slower printers (100
- CPS or less).  For example, a combination of PRTYPE 2, FSPEED 4, GP.APHICS I and
- ASPECT 4 would require the printer to print a pattern of 8 dots by 960 every 3
- seconds which would mean trouble for a 100 CPS printer.  On the other hand, a
- combination of PRTYPE 2, FSPEED 2, GRAPHICS 0 and ASPECT 2 would work, as it
- results in a pattern of dots 8 by 480 every 12 seconds.  We know the following
- combinations of dot densities and FSPEED cause trouble.
- FSPEED, 8" width (narrow)      FSPEED 13" width (wide)
- Dot Density         0    1    2    3    4         0    1    2    3    4
- 60   dpi
- 72   dpi                                                              s
- 80   dpi                                                              x
- 90   dpi                                                              x
- 96   dpi                                                         s    x
- 100  dpi                                                         s    x
- 120  dpi                                s                        x    x
- 136  dpi                                x                        x    x
- 144  dpi                                x                   s    x    x
- 160  dpi                           s    x         x    x    x    x    x
- 200  dpi                           x    x         x    x    x    x    x
- 240  dpi                      s    x    x         x    x    x    x    x
**Description:**


---

### Graphics  “n” Default: 1 (960 dots)
**Mode:** FAX Host: GR
**Parameters:**
- “n" 0 to 6 selects the FAX horizontal graphics dot density printed on the
- printer from the table below
- GRAPHICS determines the horizontal print density of the parallel Printer.  The
- GRAPHICS dot densities for each PRTYPE will be given with the PRTYPE command.
- Graphics dot-densities as a function of PRTYPE are shown below.
- Density in dote/inch (dpi) as a function of GRAPHICS and
- PRTYPE
- GRAPHICS
- PRTYPE    0        1        2        3        4        5        6
- 0-3 60 120 120 240 50 72 90
- 4-7 60 120 120 240 50 72 90
- 8-9 60 120 144 200 80 72 90
- 12-19 136 240 144 160 50 72 96
- 20-21 60 60 60 60 60 72 100
- 24-27 60 120 144 240 60 72
- 28-29 60 120
- 32-35 60 120 120 240 80 72 90
- 36 60 60 60 60 60 60 60
- 40-43 60 120 120 120 60 72 144
- 44-47 72 144 144 72 72 72 72
- 48-51 50 160 80 50 50 80 80
- In using the various GRAPHICS densities above, the user should be aware that not
- all the combinations or parameters work, especially with the slower printers (100
- CPS or less).  For example, a combination of PRTYPE 2, FSPEED 4, GP.APHICS I and
- ASPECT 4 would require the printer to print a pattern of 8 dots by 960 every 3
- seconds which would mean trouble for a 100 CPS printer.  On the other hand, a
- combination of PRTYPE 2, FSPEED 2, GRAPHICS 0 and ASPECT 2 would work, as it
- results in a pattern of dots 8 by 480 every 12 seconds.  We know the following
- combinations of dot densities and FSPEED cause trouble.
- FSPEED, 8" width (narrow)      FSPEED 13" width (wide)
- Dot Density         0    1    2    3    4         0    1    2    3    4
- 60   dpi
- 72   dpi                                                              s
- 80   dpi                                                              x
- 90   dpi                                                              x
- 96   dpi                                                         s    x
- 100  dpi                                                         s    x
- 120  dpi                                s                        x    x
- 136  dpi                                x                        x    x
- 144  dpi                                x                   s    x    x
- 160  dpi                           s    x         x    x    x    x    x
- 200  dpi                           x    x         x    x    x    x    x
- 240  dpi                      s    x    x         x    x    x    x    x
**Description:**


---

### HBaud "n"                                              Default: 1200 bauds
**Mode:** Packet                                           Host: HB
**Parameters:**
- "n"  -    values specifying the data rate in bauds from the PK-232 to the radio.
**Description:**
HBAUD sets the radio ("on-air") baud rate only in the Packet operating mode.   HBAUD has no relationship to your computer terminal program's baud rate. Available HDLC packet data rates "n" include 45, 50, 57, 75, 100, 110, 150, 200,  300, 400, 600, 1200, 2400, 4800 and 9600 bauds.  Internal modems are provided  for 45 - 1200 bauds.  At higher data rates, an external modem must be used. _______________________________________________________________________________

---

### HEAderln ON|OFF                                             Default: ON
**Mode:** Packet                                                Host: HD
**Parameters:**
- ON   -    The header in a monitored packet is printed on a separate line from 
- the text.
- OFF  -    The header and text of monitored packets are printed on the same line.
**Description:**
When HEADERLN is ON (default), the address is shown, followed by a <CR><LF> that  puts the packet text on a separate line as shown below: WX1AAA>WX2BBB: Go ahead and transfer the file. HEADERLN affects the display of monitored packets.  When HEADERLN is OFF, the  address information is shown on the same line as the packet text as shown below: WX1AAA>WX2BBB: Go ahead and transfer the file.

______________________________________________________________________________

---

### Help                                                   Immediate Command
**Mode:** Command                                          Host: Not Supported
**Description:**
_______________________________________________________________________________ While in Command Mode, type the command "H" to read the abbreviated on-line HELP  file.  Your monitor displays the following brief list: Help: AMtor     PAcket    AScii ARq       Connect   BAudot AList     Disconne  MOrse FEc       MHeard    DISPlay AChg      CStatus   CALibrat NAvtex    SIgnal    FAx       TDm       CONVerse  Trans Xmit      Rcve      Lock RESTART   RESET MDCheck   TClear You can enter Command Mode at any time to list the HELP text. _______________________________________________________________________________

---

### HEReis "n"                                        Default: $02 <CTRL-B>
**Mode:** Baudot, ASCII and AMTOR                     Host: HR
**Parameters:**
- "n"   -   Is the hex representation ($01-$7F) of the character that causes the 
- AAB string to be sent in the middle of transmitted text.
**Description:**
If you wish to send your own AAB string for identification during a transmission  simply enter the HEREIS character (default <CTRL-B>).  Also see the command AAB. _______________________________________________________________________________

---

### HId ON|OFF                                                  Default: OFF
**Mode:** Packet                                                Host: HI
**Parameters:**
- ON   -    Your PK-232 sends HDLC identification as a digipeater.
- OFF  -    Your PK-232 does not send HDLC identification.
**Description:**
Set HID ON to force your PK-232 to send an ID packet every 9.5 minutes when it's  being used as a digipeater.  Otherwise leave HID OFF (default). This identification consists of a UI-frame with your station identification  (MYCALL) and MYALIAS in the data field.  The packet is addressed to "ID". NOTE:     You cannot change the 9.5-minute automatic interval timing.

_______________________________________________________________________________

---

### HOMebbs call                                                Default: (none)
**Mode:** Packet/MailDrop                                       Host: HM
**Parameters:**
- call  -   Call Sign of your HOME BBS with which you have made prior arrangements 
- to Auto-Forward.
**Description:**
This is the Call Sign of your local or HOME BBS that you will use for Reverse  Forwarding messages.  You must make special arrangements with the system  operator of this BBS to set you up for Reverse Forwarding.  The SSID is not  compared when matching HOMEBBS to the source call sign of an incoming packet. _______________________________________________________________________________

---

### HOST "n"                                                    Default: 0
**Mode:** All                                                   Host: HO
**Parameters:**
- "n"   -   A hexadecimal value from $00 through $FF setting bits from the table 
- below that define the Host operation of the PK-232.
**Description:**
The HOST command enables the "computer-friendly" HOST communications mode, over  the PK-232's RS-232 link.  To cancel HOST mode, send 3-<CTRL-C> characters as if  exiting the Transparent mode, or type <CTRL-A> O H O N <CTRL-W>.  Sending a  Break signal will not cause the PK-232 to exit from the HOST mode. Bit 0:    Controls whether the HOST mode is ON or OFF. If bit 0 is equal to 0, HOST is OFF. If bit 0 is equal to 1, HOST is ON. Bit 1:    Controls the local MailDrop access. If bit 1 is equal to 0, then the Maildrop Send data uses the $20  block.  Read data uses the the $2F block as  before.  Monitored MXMIT data uses the $3F  (monitored receive) block type. If bit 1 is equal to 1, then the MailDrop send data uses the $60 block  type.  Read data uses the $70 block type.   Monitored MXMIT data uses the $2F (echoed)  block type to differentiate between monitored  transmitted and received frames. Bit 2:    Controls the DSP-2232's extended HOST Mode. Bits 3-7 are reserved for future use. To maintain backward compatibility with older programs written to use the ON/OFF  form of the HOST command, HOST ON is equivalent to HOST $01 described above. However programmers must note that HOST now returns a numeric value and not ON  or OFF as before. See Timewave's PK-232 Technical Manual for full information on Host Mode.

_______________________________________________________________________________

---

### HPoll ON|OFF                                                Default: ON
**Mode:** Host                                                  Host: HP
**Parameters:**
- ON   -    The HOST Mode program must poll the PK-232 for all data (default).
- OFF  -    The HOST Mode program must accept data from the PK-232 at anytime.
**Description:**
When HPOLL is ON (default) the HOST Mode program must poll the PK-232 (using the  <CTRL-A> O G G <CTRL-W>) for all data that might be available to be displayed to  the screen.  When HPOLL is OFF, the HOST Mode program must be able to accept any  data from the PK-232 whenever it becomes available. _______________________________________________________________________________

---

### Id                                                     Immediate Command
**Mode:** AMTOR/ASCII/Baudot/Packet                        Host: ID
**Description:**
_______________________________________________________________________________ In AMTOR, the ID command acts like the RCVE command, only adding a Morse ID  before going back to receive.  In ASCII and Baudot, the ID command causes a CW  ID to be sent much like an immediate version of the CWID character (CTRL-F).   Because the ID command is immediate, the message "Transmit Data Remaining" will  be displayed if any unsent data remains in the transmit buffer. In Packet, ID is an immediate command that sends a special identification  packet.  The ID command allows you to send a final identification packet when  you're taking your station off the air.  HID must also be set ON.  The  identification consists of a UI-frame, with its data field containing your  MYALIAS (if any) and your MYCALL and the word "digipeater".  The ID packet is  sent only if your PK-232 has digipeated any transmissions since the last  automatic identification. _______________________________________________________________________________

---

### ILfpack ON|OFF                                              Default: ON
**Mode:** Packet                                                Host: IL
**Parameters:**
- ON   -    The PK-232 ignores all line-feed characters received from the terminal.
- OFF  -    The PK-232 transmits all line-feeds received from the terminal.
**Description:**
The ILFPACK command permits you to control the way the PK-232 handles line-feed  characters received from your computer or terminal while in the Packet mode.

_______________________________________________________________________________

---

### IO ["n"]                                               Default: none
**Mode:** All                                              Host: IO
**Parameters:**
- "n"   -   A hexadecimal value used to access the PK-232's memory and I/O 
- locations, or read values stored at a specified ADDRESS.
**Description:**
The IO command works with the ADDRESS command (ADDRESS $aabb) and permits access  to memory and I/O locations.  Use the IO command without arguments to read an  I/O location, and with one argument $0 to $FF to write to an I/O location.  The  value in ADDRESS is not incremented after using the IO command. In ADDRESS $aabb, where "aa" (01-FF) is the device address, and "bb" is the  register address on the device. If ADDRESS is set to $00bb, the IO command reads or writes data to the device at  I/O address bb.  There is no register set-up before the access.  This command is  used primarily as a programmer's aid and is not needed for normal PK-232 use. _______________________________________________________________________________

---

### JUstify  "n"                                           Immediate Command
**Mode:**  FAX                                             Host: JU
**Parameters:**
- "n"   -   0 to 25 specifies the number of half-inches the facsimile image will 
- be moved closer to the edge of the paper.
**Description:**
The number 0-25 is in units of half-inches, or 1/16 of standard (8") paper  width.  In most cases entering JUSTIFY n will move the image to the left.  If  LEFTRITE is OFF, then JUSTIFY will move the image to the right. For example if the left-hand edge of the image is 4-1/2 inches away from the  edge of the paper, try entering JUSTIFY 8.  This will move the image 4 inches to  the left.  If this is not enough, you can always enter JUSTIFY 1, which will  move the image the additional half-inch to the left. JUSTIFY should only be needed after a manual start has been issued with the LOCK  command in the FAX mode. _______________________________________________________________________________

---

### KILONFWD  ON|OFF                                            Default: ON
**Mode:** Packet/MailDrop                                       Host: KL
**Parameters:**
- ON    -   The PK-232 kills messages after they have been Reverse Forwarded.
- OFF   -   The PK-232 does not kill messages after Reverse Forwarding.
**Description:**
Controls the disposition of a message that has been Reverse Forwarded to the  station whose call is in HOMEBBS.  If KILONFWD is ON (default), the message is  killed automatically to make room for other messages.  If KILONFWD is OFF, the  message's status is changed from "F" to "Y."

_______________________________________________________________________________

---

### KIss  "n"                                                   Default: 0
**Mode:** Packet                                                Host: KI
**Parameters:**
- "n"   -   Is a HEX number from $00 (KISS disabled) through $FF that enables the 
- KISS mode selected from the table below.
**Description:**
The KISS mode must be entered to prepare the PK-232 for KISS operation.  TCP/IP  and other special applications have been written that require the KISS mode be  enabled to operate correctly.  For normal AX.25 Packet operation, this command  should be left at 0 or OFF (default). The KISS command, formerly ON/OFF, has now been expanded to a numerical value  from $00 - $FF.  This expansion supports G8BPQ's multi-drop KISS protocol.  The  table below describes available KISS options. KISS $00: KISS disabled (formerly displayed as KISS OFF) KISS $01: Standard KISS (same as KISS ON or KISS YES) KISS $03: Extended KISS KISS $07: Extended KISS + KISS polling enabled KISS $0B: Extended KISS + KISS checksum enabled KISS $0F: Extended KISS + KISS polling and checksum enabled Note that KISS ON enables standard KISS operation for compatibility with  existing applications. Extended KISS mode adds these commands to the standard commands ($x0-$x5): $xC signifies data to be transmitted.  Unlike the $x0 command, the $xC byte is  followed by two frame ID bytes, then the data; when the TNC transmits the frame,  it notifies the host application by echoing back FEND, the $xC byte, the two  frame ID bytes, and FEND. $xE is the polling command, similar to the HOST "GG" command existing in AEA  products.  Polling makes multi-TNC KISS operation possible.  If KISS polling is  enabled, the TNC holds received data until the host application sends the poll  command.  If the TNC is holding no data, it echoes back  FEND $xE FEND.  The "x"  in "$xE" must match the number in the KISSADDR command for the TNC to respond. If KISS checksum is enabled, a checksum byte is added to the end (before the  final FEND) of all KISS blocks flowing between the TNC and the host application.   The checksum is the exclusive-OR of all other bytes between the FEND bytes,  taken before KISS escape transpositions.  A checksum is helpful when using  multiple TNCs on a marginal RS-232 link.  If the PK-232 receives a KISS block  with a bad checksum, it does not transmit the data. In KISS and Raw HDLC modes, communication activity on the RS-232 link is shown  by illuminating the STA and CON LEDs as follows: Host to TNC Communication:  STA LED illuminated. TNC to host Communication:  CON LED illuminated. HOST OFF (3 <CTRL-C>s) will force KISS OFF.  Details on the use of KISS TNC  protocol are contained in Timewave's Technical Manual for the PK-232.

_______________________________________________________________________________

---

### KISSAddr  "n"                                               Default: 0
**Mode:** Packet                                                Host: KA
**Parameters:**
- "n"   -   Is a number from 0-15 signifying the KISS address of the TNC's radio 
- port.
**Description:**
Radio port addressing is available in the high nibble of the KISS command byte.   The PK-232 compares the high nibble of the KISS command byte to KISSADDR only if  extended KISS mode is enabled.  If the command does not match KISSADDR, the TNC  takes no action.  Exception: the exit-KISS command $FF works no matter what the  value of KISSADDR or the status of extended KISS mode. _______________________________________________________________________________

---

### LAstmsg  "n"                                           Immediate Command
**Mode:**  Packet MailDrop                                 Host: LA
**Parameters:**
- "n"   -   0 to 999 specifies the message number of the last MailDrop message.
**Description:**
The number 0-999 is the number of the last message sent by a remote user or the  SYSOP to the MailDrop.  This command is handy for checking the last message sent  to your MailDrop system.  The LASTMSG command also allows the MailDrop message  counter to be set to any value, or simply reset by setting LASTMSG 0. _______________________________________________________________________________

---

### LEftrite  ON|OFF                                            Default: ON
**Mode:**  FAX                                                  Host: LR
**Parameters:**
- ON   -    The FAX signal is scanned from left to right
- OFF  -    The FAX signal is scanned from right to left
**Description:**
Occasionally you may come across FAX images that are obviously backwards.   Turning LEFTRITE OFF will reverse the scanning direction. _______________________________________________________________________________

---

### LIte ON|OFF                                                 Default: ON
**Mode:** Packet                                                Host: LI
**Parameters:**
- ON   -    The PK-232 will attempt to use the HF Packet Lite extensions.
- OFF  -    The PK-232 uses AX.25 Level 2 Version 1.0 or 2.0 protocol.
**Description:**
A Packet Lite connection is established only if both stations have LITE ON. As with the AX25L2V2 command, LITE may not be changed if the TNC is in a connected state.  Setting LITE ON overrides the AX25L2V2 setting, and the unit acts as if AX25L2V2 were ON.  See the section of the Packet Chapter (Chapter 4)  regarding Packet Lite operation and restrictions.

_______________________________________________________________________________

---

### Lock                                                   Immediate Command
**Mode:**  Morse/Baudot/AMTOR/FAX                          Host: LO
**Description:**
_______________________________________________________________________________ AMTOR and Baudot:   LOCK is an immediate command used to force a LETTERS shift  in the received data.  This can be helpful if noise has garbled the LTRS  character causing FIGURES to be displayed. FAX:      This is a manual start command for FAX.  Normally the transmitting  station starts a FAX image with sync pulses so that the receiver automatically  lines up with the edge of the paper.  If you tune in a signal too late, or there  is so much noise that the sync pulses are not detected, you can start reception  manually with the LOCK command.  If you issue a LOCK to the PK-232, you will  probably need to use the JUSTIFY command to properly align the image. Morse:    LOCK is an immediate command that instructs the PK-232 to lock its  timing to the current measured speed of a Morse signal.  The LOCK command may  improve the PK-232's ability to decode CW signals in the presence of high noise  levels. _______________________________________________________________________________

---

### MAildrop ON|OFF                                             Default: OFF
**Mode:** Packet                                                Host:  MV
**Parameters:**
- ON    -   The PK-232MBX operates as a personal packet BBS or MailDrop.
- OFF   -   The PK-232MBX only operates as a normal AX.25 Level 2 TNC (default).
**Description:**
The PK-232's MailDrop is a personal mailbox that uses a subset of the  W0RLI/WA7MBL PBBS commands.  When your MailDrop is ON, other stations can  connect to your PK-232MBX, leave messages for you or read messages from you.   Third-party messages are not accepted by your MailDrop unless 3RDPARTY is ON. See the MDCHECK, MDPROMPT, MDMON, MTEXT, MMSG and MYMAIL commands. _______________________________________________________________________________

---

### MARsdisp ON|OFF                                             Default: OFF
**Mode:** Baudot and AMTOR, RTTY                                Host: MW
**Parameters:**
- ON   -    The PK-232 translates received LTRS characters to a <CTRL-O>, and 
- FIGS characters to a <CTRL-N> and sends these to the terminal.
- OFF  -    The PK-232 operates as before in Baudot and AMTOR (default).
**Description:**
The MARSDISP command permits the Baudot and AMTOR operator to detect and display  every character including LTRS and FIGS sent by the other station.  The ACRDISP  and ALFDISP may be turned off to prevent extraneous carriage-returns and  Linefeeds from being sent to the display.  If this data is retransmitted,  ACRRTTY should be 0, and ALFRTTY should be OFF.  The <CTRL-O> and <CTRL-N>  characters will send LTRS and FIGS respectively.

_______________________________________________________________________________

---

### MAXframe "n"                                                Default: 4
**Mode:** Packet                                                Host: MX
**Parameters:**
- "n"  -    1 to 7 signifies a number of packet frames.
**Description:**
MAXFRAME limits the number of unacknowledged packets your PK-232 permits on the  radio link, and the number of contiguous packets your PK-232 will send in a  single transmission. The "best" value of MAXFRAME depends on your local channel conditions.  In most  cases of local keyboard operation, the default value of MAXFRAME 4 works well.   When the amount of traffic is heavy, the path in use is poor or you are using  many digipeaters, you can actually improve your throughput by reducing MAXFRAME. Use MAXFRAME 1 for best results on HF packet. _______________________________________________________________________________

---

### MBEll ON|OFF                                                Default: OFF
**Mode:** Packet                                                Host: ME
**Parameters:**
- ON   -    Will send 3 BELL characters to the terminal when the callsign(s) of 
- the station(s) monitored match the MFROM and MTO lists.
- OFF  -    As is, that is the PK-232 will not send any BELL characters to the 
- terminal due to MONITORED packets.
**Description:**
MBELL can be used to alert the user to the presence of particular packet  station(s) on the frequency.  For example if you want to be alerted when N7ML  comes on frequency you would set the following: MBELL ON       MONITOR 4       MFROM yes N7ML          MTO NONE When MBELL is ON, packets from and to all stations are displayed, but only those  packets matching the MFROM and MTO lists cause the bell to ring. _______________________________________________________________________________

---

### MBx call1[,call2][-"n"][ALL]                                Default: none
**Mode:** Packet                                                Host: MB
**Parameters:**
- call  -   The call signs of one or two stations to be monitored.
- "n"   -   0 to 15, indicating an optional SSID.
**Description:**
The MBX command permits you to read or record useful or needed data without  having to connect or log on to the source station(s). MBX filters the received packet data so that only packets from the selected  station(s) are shown, without headers or repeated frames.  MBX overrides normal  monitor functions and can show one or both sides of a conversation. The operation of MBX command is as follows:

MBX NONE       (Default) All monitored frames are shown with their headers. MBX ALL        Only the data fields in the I-frames and UI frames are shown.   Data from retried frames will be shown each time such a frame is  monitored.  The MFROM and MTO commands are active. MBX CALL 1     Only the data in the I and UI frames to or from CALL 1 are shown.   CALL 1 can be either the source or destination station.  Retried  frames are not shown.  The MFROM and MTO commands are ignored. MBX CALL 1,    Only the data in the I and UI frames are shown when CALL 1 is the  CALL 2     source and CALL 2 is the destination or vice-versa.  Retried  frames are not shown.  The MFROM and MTO commands are ignored. A packet connection on any channel inhibits monitoring, if MBX is not set to  "none".  MCON will only work if MBX is set to "none". Clear MBX with "%" "&" "N" "NO" "NONE" or "OFF" as arguments. _______________________________________________________________________________

---

### MCon "n"                                               Default: 0 (none)
**Mode:** Packet                                           Host: MC
**Parameters:**
- "n"  -    0 to 6 signifies various levels of monitor indications
**Description:**
Use MCON for selective monitoring of other packet traffic while connected to a  distant station.  MCON works in similar fashion to MONITOR, but affects your  display only while you are connected to another station. If MCON is set to a value between "1" and "5," frames meant for you are  displayed as though monitoring was OFF.  You'll see only the data.  If MCON is  set to "6," frames meant for you are displayed as any other monitored frame.   The headers appear together with the data. The meanings of the parameter values are: 0    Monitoring while connected is disabled. 1    Only unnumbered (UI) frames resulting from an unconnected transmission are  displayed.  Use this for an "unproto," roundtable type QSO.  This setting       also display beacons. 2    Numbered (I) frames are also displayed.  Use this to monitor connected  conversations in progress. 3    Connect request (SABM or "C") frames and disconnect (DISC or "D") frames  are also displayed with the headers. 4    Unnumbered acknowledgment (UA) of connect- and disconnect-state frames are  also displayed with either the characters "UA" or "DM" and a header. 5    Receive Ready (RR), Receive Not Ready (RNR), Reject (RJ), Frame Reject  (FRMR) and (I)-Frames are also displayed. 6    Poll/Final bit, PID and sequence numbers are also displayed.

_______________________________________________________________________________

---

### MDCheck                                                Immediate Command
**Mode:** AMTOR Packet/MailDrop                            Host: M1
**Description:**
_______________________________________________________________________________ MDCHECK is an immediate command which allows you to log on to your own MailDrop. After logging on, you can EDIT, LIST, READ, SEND or KILL MailDrop messages. To use the MDCHECK command, and your PK-232 must not be connected to or linked  to any packet or AMTOR stations.  For monitoring purposes, local access of the  MailDrop is considered a connection.  Type "B" (BYE) to quit local control of  your MailDrop. _______________________________________________________________________________

---

### MDigi ON|OFF                                                Default: OFF
**Mode:** Packet                                                Host: MD
**Parameters:**
- ON   -    I and UI frames having your call sign (MYCALL or MYALIAS) as the next 
- digipeater in the field are displayed, regardless of connected status.
- OFF  -    Normal monitoring as determined by the monitoring mode commands.
**Description:**
MDIGI permits you to display packets when another station uses your station as a  digipeater.  If you want to monitor ALL traffic that flows through your packet  station, set MDIGI ON. You may not want to see all the data passing through your station, especially if  many others use you as a digipeater.  In this case set MDIGI OFF (default). _______________________________________________________________________________

---

### MDMon ON|OFF                                                Default: OFF
**Mode:** AMTOR and Packet/MailDrop                             Host: Mm
**Parameters:**
- ON    -   Monitor a calling station's activity on your MailDrop.
- OFF   -   Normal monitoring as determined by the monitoring mode commands. 
**Description:**
Set MDMON to ON to monitor activity on your MailDrop. MDMON permits you to monitor activity on your AMTOR or packet MailDrop showing  you both sides of the QSO.  Packet headers are not shown while a caller is  logged on.  When no one is connected to your MailDrop, channel activity is  monitored according to the setting of MONITOR. Set MDMON OFF to cancel MailDrop monitoring.  Note that MailDrop connect and  link status messages will be displayed even with MDMON OFF.  These status  messages are important and allow you to see who is connected to your MailDrop.   They can be disabled however with the UBIT 13 command.  See the UBIT command for  more information .

_______________________________________________________________________________

---

### MDPrompt text                                          Default: (see text)
**Mode:** Packet/MailDrop                                  Host: Mp
**Parameters:**
- text  -   Any combination of characters and spaces up to a maximum of 80 bytes.
**Description:**
MDPROMPT is the command line sent to a calling station by your MailDrop in  response to a Send message command.  The default text is: "Subject:/Enter message, ^Z (CTRL-Z) or /EX to end" Text before the first slash is sent to the user as the subject prompt; text  after the slash is sent as the message text prompt.  If there is no slash in the  text, the subject prompt is "Subject:" and the text prompt is from MDPROMPT. _______________________________________________________________________________

---

### MEmory "n"                                                  Default: none
**Mode:** All                                                   Host: MM
**Parameters:**
- "n"   -   A hexadecimal value used to access the PK-232's memory locations, or 
- read values stored at a specified ADDRESS.
**Description:**
The MEMORY command works with the ADDRESS command (ADDRESS $aabb) and permits  access to memory locations.  Use the Memory command without arguments to read a  memory, and with one argument $0 to $FF to write to a memory location.  The  value in ADDRESS is incremented after using the MEMORY command.  _______________________________________________________________________________

---

### MFIlter n1[,n2[,n3[,n4]]]                                   Default: $80
**Mode:** Morse, Baudot ASCII, AMTOR and Packet                 Host: MI
**Parameters:**
- "n"  -    0 to $80 (0 to 128 decimal) specifies an ASCII character code.
- Up to four characters may be specified separated by commas.
**Description:**
Use MFILTER to select up to 4 characters to be "filtered," or excluded from  Morse, Baudot, ASCII, AMTOR and monitored packets.  Parameters "n1," - "n4" are  the ASCII codes for the characters you want to filter.  The special value of $80  (default) filters all characters above $7F and all control-characters except  carriage-return ($0D), linefeed ($0A), and TAB ($09). _______________________________________________________________________________

---

### MFrom ALL/NONE or YES/NO call1[,call2..]                    Default: ALL
**Mode:** Packet                                                Host: MF
**Parameters:**
- call  -   ALL/NONE or YES_list/NO_list (list of up to eight call signs, 
- separated by commas).
**Description:**
MFROM determines what packets are monitored.  To monitor all packets, set MFROM  to ALL.  To stop any packets from being displayed, set MFROM and MTO to NONE. To display packets from one or more specific stations, type MFROM YES followed  by a list of call signs you WANT to monitor packets from.  To hide packets from  one or more specific stations, type MFROM NO followed by a list of call signs  you want NOT to monitor packets from.  When using MFROM, set MTO to NONE. You can include optional SSIDs specified as "-n" after the call sign.  If MFROM  is set to "NO N6IA," any combination N6IA-0,...N6IA-15 will NOT be monitored.   If MFROM is set to "YES N6IA-1," then only N6IA-1 will be monitored. When MFROM and MTO contain different arguments, the following priority applies: 1.   ALL,      2.   NO_list,       3.   YES_list,      4.   NONE Clear MFROM with "%" "&" or "OFF" as arguments. _______________________________________________________________________________

---

### MHeard                                                 Immediate Command
**Mode:** Packet/AMTOR MailDrop                            Host: MH
**Description:**
_______________________________________________________________________________ MHEARD is an immediate command that displays a list of up to 18 most recently  heard stations.  Stations that are heard directly are marked with a * in the  heard log.  Stations that have been repeated by a digipeater are not marked. When DAYTIME has been set, entries in the heard log are time stamped.  When  DAYSTAMP is ON the date is also shown.  An example of the MHEARD display is  shown below: DAYSTAMP ON                             DAYSTAMP OFF 05-Jul-86  21:42:27  WA1FJW             21:42:27  WA1FJW 05-Jul-86  21:42:24  WA1IXU*            21:42:24  WA1IXU* Clear the MHEARD list with a "%", "&", "N," "NO," "NONE" or "OFF" as arguments. _______________________________________________________________________________

---

### MId "n"                                                Default: 0 (00 sec.)
**Mode:** Packet                                           Host: Mi
**Parameters:**
- "n"  -    0 - 250 specifies the morse ID timing in units of 10 second intervals.
- 0 (zero) disables this function.
**Description:**
If "n" is set to some value from 1 to 250, the PK-232 will periodically issue a  20 wpm Morse ID.  For example, an MID of 177 would cause a Morse ID every 1,770  seconds (29.5 minutes).  A Morse ID will only be transmitted if a packet was  sent since the last Morse ID.  The Morse ID uses TXDELAY, PPERSIST, and DCD. If MID is set to a value other than 0, ID will force a Morse ID immediately. If both HID and MID are active, the Morse ID will be sent first. MID normally sends a Morse ID using on/off keying of the low tone.  If FSK  keying of both tones is desired to prevent stations from transmitting over your  Morse ID, see the UBIT 12 command.

_______________________________________________________________________________

---

### MMsg ON|OFF                                                 Default: OFF
**Mode:** Packet/AMTOR MailDrop                                 Host: MU
**Parameters:**
- ON  -     The stored MTEXT message is sent as the first response after an AMTOR 
- link or Packet connect to the MailDrop is established.
- OFF -     The MTEXT message is not sent at all.
**Description:**
MMSG enables or disables automatic transmission of the MTEXT message when your  AMTOR or Packet MailDrop links with another station. _______________________________________________________________________________

---

### Monitor "n"                                  Default: 4 (UA DM C D I UI)
**Mode:** Packet                                 Host: MN
**Parameters:**
- "n"  -    0 to 6 signifies various levels on monitor indications
**Description:**
The Monitor command determines what kind of packets on a channel are displayed  when the PK-232 is NOT connected to any other packet stations. The meanings of the parameter values are: 0    All packet monitoring functions are disabled. 1    Only unnumbered (UI) frames resulting from an unconnected transmission are  displayed.  Use this for an "unproto," round-table type QSO.  This  setting also displays beacons. 2    Numbered (I) frames are also displayed.  I-frames are numbered in order of  generation and result from a connected transmission.  Use this to monitor  connected conversations in progress. 3    Connect request (SABM or "C") frames and disconnect (DISC or "D") frames  are also displayed with the headers. 4    Unnumbered acknowledgment (UA) of connect- and disconnect-state frames are  also displayed with either the characters "UA" or "DM" and a header. 5    Receive Ready (RR), Receive Not Ready (RNR), Reject (RJ), Frame Reject  (FRMR) and (I)-Frames are also displayed. 6    Poll/Final bit, PID and sequence numbers are also displayed. _______________________________________________________________________________

---

### MOrse                                                  Immediate Command
**Mode:** Command                                          Host: MO
**Description:**
_______________________________________________________________________________ MORSE is an immediate command that switches your PK-232 into the Morse mode. Unless you change MSPEED, your PK-232 uses the default Morse transmit speed  value of 20 WPM.

_______________________________________________________________________________

---

### MProto ON|OFF                                               Default: OFF
**Mode:** Packet                                                Host: MQ
**Parameters:**
- ON   -    Monitors all I and UI frames as before.
- OFF  -    Monitors only those I and UI frames with a PID byte of $F0.
**Description:**
This is in response to NET/ROM, which sends frames that have a PID of $CF, and  that contain Control characters.  If you want to monitor every frame including  those used by NET/ROM, you must turn MPROTO ON. _______________________________________________________________________________

---

### MRpt ON|OFF                                                 Default: ON
**Mode:** Packet                                                Host: MR
**Parameters:**
- ON   -    Show digipeater path in the packet header.
- OFF  -    Show only originating and destination stations in the packet header.
**Description:**
MRPT affects the way monitored packets are displayed.  When MRPT is ON  (default), the call signs of all stations in the digipeat path are displayed.   Call signs of stations heard directly are flagged with an asterisk (*) as shown: W2JUP-4*>WA1IXU>W1AW-5>W1AW-4 <I;0,3>: When MRPT is OFF, only the originating station and the destination stations are  displayed are displayed in the monitored packet header as shown below: W2JUP-4*>W1AW-4 <I;0,3>: _______________________________________________________________________________

---

### MSPeed "n"                                             Default: 20 WPM
**Mode:** Morse                                            Host: MP
**Parameters:**
- "n"   -   5 to 99 signifies your PK-232's Morse transmit speed.
**Description:**
The MSPEED command sets the Morse code keying (transmit) speed for your PK-232.   The slowest available Morse code speed is 5 words per minute.  When using Morse  speeds between 5 and 14 WPM, the transmitted code is sent with Farnsworth  spacing at a character speed of 15 words per minute.  The spacing between  characters is lengthened to produce an overall code rate of 5 to 14 WPM. _______________________________________________________________________________

---

### MStamp ON|OFF                                               Default: OFF
**Mode:** Packet                                                Host: MS
**Parameters:**
- ON   -    Monitored frames ARE time stamped.
- OFF  -    Monitored frames ARE NOT time stamped.
**Description:**
The MSTAMP command activates time stamping of monitored packets.  When your  PK-232's internal software clock is set, date and time information is available  for automatic logging of packet activity and other applications. Remember to set the date and time with the DAYTIME command. When MSTAMP is OFF, the packet header display looks like this: W2JUP-4*>KA2EYW-1>AI2Q <I;2,2>: When MSTAMP is ON and DAYSTAMP is OFF, the display looks like this: 22:51:33  W2JUP-4*>KA2EYW-1>AI2Q <I;2,2>: _______________________________________________________________________________

---

### MTExt text                                        Default: See sample
**Mode:** AMTOR/Packet MailDrop                       Host: Mt
**Parameters:**
- text      Any printable message up to a maximum of 120 characters.
**Description:**
MTEXT is the "MailDrop automatic answer" text similar to CTEXT.  If MMSG is ON,  the MTEXT message is sent when a station links to your AMTOR or Packet MailDrop. The default text is: "Welcome to my AEA PK-232M maildrop. Type H for help." MTEXT can be cleared with a "%", "&", "N," "NO," "NONE" or "OFF" as arguments. _______________________________________________________________________________

---

### MTo ALL/NONE or YES/NO call1[,call2..]                      Default: none
**Mode:** Packet                                                Host: MT
**Parameters:**
- call  -   ALL/NONE or YES_list/NO_list (list of up to eight call signs, 
- separated by commas).
**Description:**
MTO determines what packets are monitored.  To monitor all packets, set MTO to  ALL.  To stop any packets from being displayed, set MTO and MFROM to NONE. To display packets TO one or more specific stations, type MTO YES followed by a  list of call signs you WANT to monitor packets to.  To hide packets TO one or  more specific stations, type MTO NO followed by a list of call signs you want  NOT to monitor packets to.  When using MTO, set MFROM to NONE. You can include optional SSIDs specified as "-n" after the call sign.  If MTO  is set to "NO N6IA," any combination N6IA-0,...N6IA-15 will NOT be monitored.  If MTO is set to "YES N6IA-1," then only N6IA-1 will be monitored. When MFROM and MTO contain different arguments, the following priority applies: 1.   ALL,      2.   NO_list,       3.   YES_list,      4.   NONE Clear MTO with "%" "&" or "OFF" as arguments.

_______________________________________________________________________________

---

### MWeight "n"                                                 Default: 10
**Mode:** All except Packet                                     Host: Mw
**Parameters:**
- "n"  -    5 to 15, specifies roughly 10 times the ratio of one dot length to one 
- inter-element space length in transmitted Morse code.
**Description:**
A value of 10 results in a 1:1 dot-space ratio.  A setting of 5 results in a  0.5:1 ratio, while a setting of 15 (maximum) results in a 1.5:1 ratio. MWEIGHT applies only to the Morse transmit mode and the CW ID in all modes  except packet.  MWEIGHT does not affect the code output by the MID command. _______________________________________________________________________________

---

### MXmit ON|OFF                                                Default: OFF
**Mode:** Packet                                                Host: Mx
**Parameters:**
- ON  -     Monitor transmitted packets in the same manner as received packets.
- OFF -     Do not monitor transmitted packets.
**Description:**
When MXMIT is ON, transmitted packets are monitored in the same manner as  received packets.  The monitoring of transmitted packets is subject to the  settings of MONITOR, MCON, MFROM, MTO, MRPT and TRACE.  Most transmitted packets  occur during connections so MCON should probably be set to a non-zero value. _______________________________________________________________________________

---

### MYAlias call[-n]                                            Default: none
**Mode:** Packet                                                Host: MA
**Parameters:**
- call  -   Alternate packet digipeater identity of your PK-232
- "n"   -   0 to 15, an optional substation ID (SSID)
**Description:**
MYALIAS specifies an alternate call sign (in addition to the call sign specified  in MYCALL) for use as a digipeater only.  In some areas wide-coverage digipeater  operators change their call sign to a shorter and easier to remember identifier. _______________________________________________________________________________

---

### MYALTcal aaaa                                               Default: none 
**Mode:** AMTOR                                                 Host: MK
**Parameters:**
- aaaa  -   Your alternate SELective CALling code (SELCALL)
**Description:**
Use the MYALTCAL command to specify an your alternate SELCALL which, under  certain conditions, may be convenient or necessary.  You can enter an additional  SELCALL code not related to your call sign.  The alternate SELCALL can be any  four alphabetical characters, or can be numeric strings of either four or five  numbers.  MYALTCAL is generally used for special applications such as receiving  network or group broadcasts in AMTOR Mode B Selective (Bs or SELFEC).

_______________________________________________________________________________

---

### MYcall call [-"n"]                                     Default: PK232
**Mode:** Packet                                           Host: ML
**Parameters:**
- call  -   Your call sign
- "n"   -   0 - 15, indicating an optional substation ID, (SSID)
**Description:**
Use the MYCALL command to load your call sign into your PK-232. The "PK232" default call sign is present in your PK-232's ROM when the system is  manufactured.  This "artificial call" must be changed for packet operation. Two or more stations cannot use the same call and SSID on the air at the same  time.  Use a different SSID if you have more than one packet station on the air. _______________________________________________________________________________

---

### MYIdent aaaaaaa[aa]                                         Default: none 
**Mode:** AMTOR                                                 Host: Mg
**Parameters:**
- aaaaaaa[aa] -  Specifies the 7-character SELCALL as described in CCIR Rec. 625.
**Description:**
The MYIDENT command holds the CCIR Rec. 625 seven-character AMTOR SELCALL. Amateurs may simply enter their callsign and the PK-232 will automatically  translate it to a 7-character SELCALL as shown below: MYIDENT  KA1XYZ    becomes  MYIDENT  KAIXYZZ. If aaaaaaa[aa] is nine numerals, the unit translates the numerals to seven  letters according to Recommendation 491.  If aaaaaaa consists of seven legal  characters, the PK-232 accepts the characters without modifying them.  Legal  SELCALL characters for Rec. 625 are the letters A-Z except G, H, J, L, N and W. If aaaaaaa is a string of characters of any length that includes illegal  characters, the PK-232 will do the following translation on the characters: 0: O           4: Y           8: B           J: U            1: I           5: S           9: P           L: F 2: Z           6: D           G: C           N: V 3: E           7: T           H: K           W: M All other letters are unchanged. If MYSELCAL and MYIDENT are both none (defaults), no incoming ARQ or SELFEC  call can establish communications with the unit.

---

### MYMail call[-“n”] Default: none
**Mode:** Packet/MailDrop Host: Me
**Parameters:**
- Call    -      The Call Sign you wish to use for the MailDrop.
- “n”      -     Numeral indicating an optional substation ID (SSID) or extension.
- Call is the call sign of the MailDrop, default -none.
- “Call” may have an optional SSID, and must not be the same call sign and SSID as
- MYCALL.  If you do not set MYMAIL, the MailDrop will use the same call sign and
- SSID as entered in MYCALL.  For example, if you have set MYCALL to N7ML then
- MYMAIL may be N7ML-1 through N7ML-15.  You can use the CTEXT and MTEXT messages
- to inform other stations who connect of your MYCALL and MYMAIL call signs.
- MYPTcall callsign Default PK232
- Mode: Pactor Host:   Mf
- Use the MYPTCALL command to load your call sign Into your PK-232.
- If you have not loaded a call into the PK-232 with MYPTCALL, the call loaded in
- MYCALL will be used.  The difference between MYCALL and MYPTCALL is that MYCALL
- allows only the dash (-) to be used while MYPTCALL will allow any punctuation
- with the call.
- If calls have not been loaded into either MYCALL or MYPTCALL, the PK-232 will not
- allow transmission on Pactor.  An error message “Need MYCALL" will be displayed
- if transmission is attempted.
- Example:
- MYPTCALL K6RFK/ZL <Enter>
- KYSelcal ease Default: none
- Mode: AMTOR Host: MG
- Parameters:
- sees -     Specifies your SELective CALling code (SELCALL)
- Use the MYSELCAL command to enter the SELCALL (SELective CALLing) code required
- in AMTOR ARQ (Mode A) and SELFEC operating modes.  MYSELCAL is a unique character
- string which must contain four alphabetic characters and is normally derived from
- your call sign.
- Amateurs may simply enter their callsign and the PK-232 will automatically
- translate it to a 4-character SELCALL using the grouping table below:
- GROUP               CALL         SELCALL
- I by2 WIXY WWXY
- I by3 W1XYZ WXYZ
- 2 by1 ABIX AABX
- 2 by2 ABLXY ABXY
- 2 by3 KAIXYZ KXYZ
- Although the convention is to form the SELCALL from the call sign, your PK-232
- can include any AMTOR character in the SELCALL.  In accordance with CCIR
- Recommendation 491, four- or five-digit numbers may be entered; the PK-232
- automatically translates the numeric entry to your four-letter alpha SELCALL.
- NAVMsg all, none, Yes/No (letters)                     Default: All
- Mode: NAVTEX Host: NM 
- Parameters:
- letters all, none, YES List, NO List.  List of up to 13 letters which may
- or may not be separated by spaces, commas or TABS.
- NAVMSG uses Letter arguments to determine which NAVTEX messages your PK-232 will
- print.  NAVTEX messages are grouped into classes by the second letter in the
- Preamble.  The NAVMSG Command allows ALL, NONE or a list of up to 13 letters
- representing message types to be Monitored or Rejected.
- NAVY-SG may be cleared with “%”, “&” or "OFF” as arguments.
- This page is blank
- NAVStn  all,  none,  Yes/No  (letters) Default: All
- Mode: NAVTEX Host: NS
- Parameters:
- Letters — all, none, YES List, NO List.  List of up to 13 letters which
- may or may not be separated by spaces, commas or TABS.
- The NAVSTN command uses letter arguments to determine which NAVTEX transmitting
- stations the PK-232 will print.  NAVTEX transmitters are identified by the 26
- letters of the alphabet A-Z.  The NAVSTN Command allows ALL, NONE or a list of up
- to 13 letters representing NAVTEX transmitting stations to be Monitored or
- Rejected.
- NAVSTN may be cleared with  “%”, “&” or “OFF” as arguments.
- Navtex  Immediate Command
- Mode: All Host: MA
- NAVTEX is an immediate command that switches your PK-232 into the NAVTEX receive
- mode.  The PK-232 can accept only, or lock-out certain message classes and
- transmitting stations with the NAVMSG and NAVSTN commands described above.
- For logging purposes, NAVTEX mode uses the setting of DAYTIME to print the date
- and/or time in front of the preamble if MSTAMP and DAYSTAMP are ON.
- NEwmode ON/OFF Default: ON
- Mode: All Host: NE
- Parameters:
- ON — The PK-232 automatically returns to the Command Mode at disconnect.  
- OFF  — The PK-232 does not return to Command Mode at disconnect.
- Your PK-232 always switches to a data transfer mode at the time of connection,
- unless NOMODE is ON.  NEWMODE determines how your PK-232 behaves when the link is
- broken.
- When NEWMODE is ON (default) and the link is disconnected, or if the connect
- attempt fails, your PK-232 returns to Command Mode.  If NEWMODE is OFF and the
- link is disconnected, your PK-232 remains in Converse or Transparent Mode unless
- you have forced it to return to Command Mode.
**Description:**


---

### MYPTcall callsign Default PK232
**Mode:** Pactor Host:   Mf
**Description:**
Use the MYPTCALL command to load your call sign Into your PK-232. If you have not loaded a call into the PK-232 with MYPTCALL, the call loaded in MYCALL will be used.  The difference between MYCALL and MYPTCALL is that MYCALL allows only the dash (-) to be used while MYPTCALL will allow any punctuation with the call. If calls have not been loaded into either MYCALL or MYPTCALL, the PK-232 will not allow transmission on Pactor.  An error message “Need MYCALL" will be displayed if transmission is attempted. Example: MYPTCALL K6RFK/ZL <Enter>

---

### KYSelcal ease Default: none
**Mode:** AMTOR Host: MG
**Parameters:**
- sees -     Specifies your SELective CALling code (SELCALL)
- Use the MYSELCAL command to enter the SELCALL (SELective CALLing) code required
- in AMTOR ARQ (Mode A) and SELFEC operating modes.  MYSELCAL is a unique character
- string which must contain four alphabetic characters and is normally derived from
- your call sign.
- Amateurs may simply enter their callsign and the PK-232 will automatically
- translate it to a 4-character SELCALL using the grouping table below:
- GROUP               CALL         SELCALL
- I by2 WIXY WWXY
- I by3 W1XYZ WXYZ
- 2 by1 ABIX AABX
- 2 by2 ABLXY ABXY
- 2 by3 KAIXYZ KXYZ
- Although the convention is to form the SELCALL from the call sign, your PK-232
- can include any AMTOR character in the SELCALL.  In accordance with CCIR
- Recommendation 491, four- or five-digit numbers may be entered; the PK-232
- automatically translates the numeric entry to your four-letter alpha SELCALL.
- NAVMsg all, none, Yes/No (letters)                     Default: All
- Mode: NAVTEX Host: NM 
- Parameters:
- letters all, none, YES List, NO List.  List of up to 13 letters which may
- or may not be separated by spaces, commas or TABS.
- NAVMSG uses Letter arguments to determine which NAVTEX messages your PK-232 will
- print.  NAVTEX messages are grouped into classes by the second letter in the
- Preamble.  The NAVMSG Command allows ALL, NONE or a list of up to 13 letters
- representing message types to be Monitored or Rejected.
- NAVY-SG may be cleared with “%”, “&” or "OFF” as arguments.
- This page is blank
- NAVStn  all,  none,  Yes/No  (letters) Default: All
- Mode: NAVTEX Host: NS
- Parameters:
- Letters — all, none, YES List, NO List.  List of up to 13 letters which
- may or may not be separated by spaces, commas or TABS.
- The NAVSTN command uses letter arguments to determine which NAVTEX transmitting
- stations the PK-232 will print.  NAVTEX transmitters are identified by the 26
- letters of the alphabet A-Z.  The NAVSTN Command allows ALL, NONE or a list of up
- to 13 letters representing NAVTEX transmitting stations to be Monitored or
- Rejected.
- NAVSTN may be cleared with  “%”, “&” or “OFF” as arguments.
- Navtex  Immediate Command
- Mode: All Host: MA
- NAVTEX is an immediate command that switches your PK-232 into the NAVTEX receive
- mode.  The PK-232 can accept only, or lock-out certain message classes and
- transmitting stations with the NAVMSG and NAVSTN commands described above.
- For logging purposes, NAVTEX mode uses the setting of DAYTIME to print the date
- and/or time in front of the preamble if MSTAMP and DAYSTAMP are ON.
- NEwmode ON/OFF Default: ON
- Mode: All Host: NE
- Parameters:
- ON — The PK-232 automatically returns to the Command Mode at disconnect.  
- OFF  — The PK-232 does not return to Command Mode at disconnect.
- Your PK-232 always switches to a data transfer mode at the time of connection,
- unless NOMODE is ON.  NEWMODE determines how your PK-232 behaves when the link is
- broken.
- When NEWMODE is ON (default) and the link is disconnected, or if the connect
- attempt fails, your PK-232 returns to Command Mode.  If NEWMODE is OFF and the
- link is disconnected, your PK-232 remains in Converse or Transparent Mode unless
- you have forced it to return to Command Mode.
**Description:**


---

### NAVMsg all, none, Yes/No (letters)                     Default: All
**Mode:** NAVTEX Host: NM 
**Parameters:**
- letters all, none, YES List, NO List.  List of up to 13 letters which may
- or may not be separated by spaces, commas or TABS.
- NAVMSG uses Letter arguments to determine which NAVTEX messages your PK-232 will
- print.  NAVTEX messages are grouped into classes by the second letter in the
- Preamble.  The NAVMSG Command allows ALL, NONE or a list of up to 13 letters
- representing message types to be Monitored or Rejected.
- NAVY-SG may be cleared with “%”, “&” or "OFF” as arguments.
- This page is blank
- NAVStn  all,  none,  Yes/No  (letters) Default: All
- Mode: NAVTEX Host: NS
- Parameters:
- Letters — all, none, YES List, NO List.  List of up to 13 letters which
- may or may not be separated by spaces, commas or TABS.
- The NAVSTN command uses letter arguments to determine which NAVTEX transmitting
- stations the PK-232 will print.  NAVTEX transmitters are identified by the 26
- letters of the alphabet A-Z.  The NAVSTN Command allows ALL, NONE or a list of up
- to 13 letters representing NAVTEX transmitting stations to be Monitored or
- Rejected.
- NAVSTN may be cleared with  “%”, “&” or “OFF” as arguments.
- Navtex  Immediate Command
- Mode: All Host: MA
- NAVTEX is an immediate command that switches your PK-232 into the NAVTEX receive
- mode.  The PK-232 can accept only, or lock-out certain message classes and
- transmitting stations with the NAVMSG and NAVSTN commands described above.
- For logging purposes, NAVTEX mode uses the setting of DAYTIME to print the date
- and/or time in front of the preamble if MSTAMP and DAYSTAMP are ON.
- NEwmode ON/OFF Default: ON
- Mode: All Host: NE
- Parameters:
- ON — The PK-232 automatically returns to the Command Mode at disconnect.  
- OFF  — The PK-232 does not return to Command Mode at disconnect.
- Your PK-232 always switches to a data transfer mode at the time of connection,
- unless NOMODE is ON.  NEWMODE determines how your PK-232 behaves when the link is
- broken.
- When NEWMODE is ON (default) and the link is disconnected, or if the connect
- attempt fails, your PK-232 returns to Command Mode.  If NEWMODE is OFF and the
- link is disconnected, your PK-232 remains in Converse or Transparent Mode unless
- you have forced it to return to Command Mode.
**Description:**


---

### NAVStn  all,  none,  Yes/No  (letters) Default: All
**Mode:** NAVTEX Host: NS
**Parameters:**
- Letters — all, none, YES List, NO List.  List of up to 13 letters which
- may or may not be separated by spaces, commas or TABS.
- The NAVSTN command uses letter arguments to determine which NAVTEX transmitting
- stations the PK-232 will print.  NAVTEX transmitters are identified by the 26
- letters of the alphabet A-Z.  The NAVSTN Command allows ALL, NONE or a list of up
- to 13 letters representing NAVTEX transmitting stations to be Monitored or
- Rejected.
- NAVSTN may be cleared with  “%”, “&” or “OFF” as arguments.
- Navtex  Immediate Command
- Mode: All Host: MA
- NAVTEX is an immediate command that switches your PK-232 into the NAVTEX receive
- mode.  The PK-232 can accept only, or lock-out certain message classes and
- transmitting stations with the NAVMSG and NAVSTN commands described above.
- For logging purposes, NAVTEX mode uses the setting of DAYTIME to print the date
- and/or time in front of the preamble if MSTAMP and DAYSTAMP are ON.
- NEwmode ON/OFF Default: ON
- Mode: All Host: NE
- Parameters:
- ON — The PK-232 automatically returns to the Command Mode at disconnect.  
- OFF  — The PK-232 does not return to Command Mode at disconnect.
- Your PK-232 always switches to a data transfer mode at the time of connection,
- unless NOMODE is ON.  NEWMODE determines how your PK-232 behaves when the link is
- broken.
- When NEWMODE is ON (default) and the link is disconnected, or if the connect
- attempt fails, your PK-232 returns to Command Mode.  If NEWMODE is OFF and the
- link is disconnected, your PK-232 remains in Converse or Transparent Mode unless
- you have forced it to return to Command Mode.
**Description:**


---

### Navtex  Immediate Command
**Mode:** All Host: MA
**Description:**
NAVTEX is an immediate command that switches your PK-232 into the NAVTEX receive mode.  The PK-232 can accept only, or lock-out certain message classes and transmitting stations with the NAVMSG and NAVSTN commands described above. For logging purposes, NAVTEX mode uses the setting of DAYTIME to print the date and/or time in front of the preamble if MSTAMP and DAYSTAMP are ON.

---

### NEwmode ON/OFF Default: ON
**Mode:** All Host: NE
**Parameters:**
- ON — The PK-232 automatically returns to the Command Mode at disconnect.  
- OFF  — The PK-232 does not return to Command Mode at disconnect.
- Your PK-232 always switches to a data transfer mode at the time of connection,
- unless NOMODE is ON.  NEWMODE determines how your PK-232 behaves when the link is
- broken.
- When NEWMODE is ON (default) and the link is disconnected, or if the connect
- attempt fails, your PK-232 returns to Command Mode.  If NEWMODE is OFF and the
- link is disconnected, your PK-232 remains in Converse or Transparent Mode unless
- you have forced it to return to Command Mode.
**Description:**


---

### NOmode ON|OFF                                               Default: OFF
**Mode:** All                                                   Host: NO
**Parameters:**
- ON   -    The PK-232 switches modes only upon explicit command.
- OFF  -    The PK-232 changes modes according to NEWMODE.
**Description:**
When NOMODE is OFF (default), your PK-232 switches modes automatically according  to NEWMODE.  When NOMODE is ON your PK-232 never switches from Converse or  Transparent Mode to Command Mode (or vice versa) by itself.  Only specific  commands (CONVERSE, TRANS, or <CTRL-C>) typed by you change the operating mode. ______________________________________________________________________________

---

### NUCr ON|OFF                                                 Default: OFF
**Mode:** All                                                   Host: NR
**Parameters:**
- ON   -    <NULL> characters ARE sent to the terminal following <CR> characters.
- OFF  -    <NULL> characters ARE NOT sent to the terminal following <CR>s. 
**Description:**
The NULLS command sets the number of <NULL> characters that will be sent. Some older printer-terminals require extra time for the printing head to do a  carriage return and line feed.  NUCR ON solves this problem by making your  PK-232 send <NULL> characters (ASCII code $00) to your computer or terminal. _______________________________________________________________________________

---

### NULf ON|OFF                                                 Default: OFF
**Mode:** All                                                   Host: NF
**Parameters:**
- ON   -    <NULL> characters are sent to the terminal following <LF> characters.
- OFF  -    <NULL> characters are not sent to the terminal following <LF>s.
**Description:**
Some older printer-terminals require extra time for the printing head to do a  carriage return and line feed.  NULF ON solves this problem my making your  PK-232 send <NULL> characters (ASCII code $00) to your computer or terminal. The  NULLS command sets the number of <NULL> characters that will be sent. _______________________________________________________________________________

---

### NULLs "n"                                              Default: 0 (zero)
**Mode:** All                                              Host: NU
**Parameters:**
- "n"  -    0 to 30 specifies the number of <NULL> characters to be sent to your 
- computer or terminal after <CR> or <LF> when NUCR or NULF are set ON.
**Description:**
NULLS specifies the number of <NULL> characters (ASCII $00) to be sent to the  terminal after a <CR> or <LF> is sent.  NUCR and/or NULF must be set to indicate  whether nulls are to be sent after <CR>, <LF> or both.  The null characters are  sent from your PK-232 to your computer only in Converse and Command Modes.

_______________________________________________________________________________

---

### Nums                                                   Immediate Command
**Mode:** Baudot, AMTOR, TDM                               Host: NX
**Description:**
_______________________________________________________________________________ In Baudot, AMTOR and TDM receive, the NUMS command, or "N" will force the PK-232  into the FIGS case. _______________________________________________________________________________

---

### OK                                                     Immediate Command
**Mode:**  SIGNAL                                          Host: OK
**Description:**
_______________________________________________________________________________ OK normally follows the SIGNAL command after it has determined the class and  speed of the received station.  Typing OK will change the commands RXREV, RBAUD  or ABAUD and OPMODE to their proper value.  If the SIGNAL command did not reveal  any useful information, typing OK will produce the "?bad" error message. _______________________________________________________________________________

---

### Opmode                                                 Immediate Command
**Mode:**  Command                                         Host: OP
**Description:**
_______________________________________________________________________________  OPMODE is an immediate command that shows the PK-232's current mode of operation  and system status.  Opmode also displays the MORSE speed when in the Morse mode. Use the OPMODE command at any time when your PK-232 is in the Command Mode to  display the present operating mode.  Here is a typical example: cmd:o OPmode   AScii      RCVE _______________________________________________________________________________

---

### PAcket                                                 Immediate Command
**Mode:** Command                                          Host: PA
**Description:**
_______________________________________________________________________________ Use the PACKET command to switch your PK-232 into packet radio mode from any  other operating mode. _______________________________________________________________________________

---

### PACLen "n"                                                  Default: 128
**Mode:** Packet                                                Host: PL
**Parameters:**
- "n"  -    0 to 255 specifies the maximum length of the data portion of a packet.
- 0    -    Zero is equivalent to 256.
**Description:**
PACLEN sets the maximum number of data bytes to be carried in each packet's  "information field."  Most keyboard-to-keyboard operators use the default value  of 128 bytes for routine VHF/UHF packet services.  Your PK-232 automatically  sends a packet when the number of characters you type for a packet equals "n." Reduce PACLEN to 64, or even 32 when working "difficult" HF radio paths.

---

### PACTime EVERY/AFTER -n- Default:AFTER 10 (1000 msec.
**Mode:** Packet Host: PT
**Parameters:**
- "n" - 01 to 250 specifies 100-millisecond intervals.
- EVERY - Packet time-out occurs every “n”' times 100 milliseconds.
- AFTER - Packet time-out-occurs when “n” times 100 milliseconds elapse without
- input from the computer or terminal.
- The PACTIME parameter sets the amount of time in 100 msec increments that the PK-
- 232 will wait for a character to be received on the serial port before sending a
- packet in Transparent Mode.  The PACTIME parameter is always used in Transparent
- Mode but is also used in Converse Mode if CPACTIME is ON.
- When EVERY is specified, the characters you type are “packetized” every “n” times
- 100 milliseconds.  When AFTER is specified, the characters you type are
- “Packetized” when input from the terminal stops for “n” times 100 milliseconds.
- The PACTIME timer is not started until the first character or byte is entered.  A
- value of 0 (zero) for "n" means packets are sent with no wait time.
- PACTor (PT for short) Immediate Command 
- Mode: Command Host: Pt
- Pactor is an immediate command that switches the PK-232 into the Pactor mode of
- operation.  This mode is an option.
- Pactor is a mode of data communication that combines some of the features of both
- AMTOR and packet.  The abbreviated command is PT.  It has both a linked mode
- called ARQ and a non-linked mode called unproto.  See Chapter 11 for details.
- PARity "n" Default: 3 (even)
- Mode: All Host: PR
- Parameters:
- “n”      — 0 to 3 selects a parity option from the table below.
- PARITY sets the PK-232's parity for RS-232 terminal according to the table below:
- 0 no parity 2 no parity
- 1 odd parity 3 even parity
- The parity bit, if present, is stripped automatically on input and is not checked
- in command and Converse Modes.  In Transparent Mode all eight bits (including
- parity) are transmitted.
- The change will not take effect until a RESTART is performed.  Be sure to change
- the computer or terminal to the same parity setting.
- PASs  "n” Default: $16 <CTRL-V>
- Mode: Packet, ASCII and Pactor Host:  PS
- Parameters:
- 0 to $7F (O to 127 decimal) specifies an ASCII character code.
- PASS selects the ASCII character used for the "pass' input editing commands.  The
- parameter “n” is the ASCII code for the character used to pass editing characters
- (default <CTRL-V>).  The PASS character signals that the following character is
- to be included in a packet Pactor or ASCII text string.
- PK-232 OPERATING MANUAL             COMMAND SUMMARY
- The rest of this page is blank
- PASSAll ON/OFF Default: OFF
- Mode: Packet Host: PK
- Parameters:
- ON -Your PK-232 will accept packets with valid or invalid  CRCS.
- OFF -Your PK-232 will accept packets with valid CRCs only.
- PASSALL turns off the PK-232's packet error-detecting mechanism and displays
- received packets with invalid CRCS.  PASSALL is normally turned OFF (default);
- which ensures that packet data la error-free by rejecting packets with invalid
- CRC fields.  When PASSALL is ON, packets are displayed, despite CRC errors.  The
- MHEARD logging is disabled since the call signs detected may be incorrect.
- PErsist “n" Default: 63
- Mode: Packet Host: PE
- Parameters:
- “n"  —  0 to 255 specifies the threshold for a random attempt to transmit.
- The PERSIST parameter works with the PPERSIST and SLOTTIME parameter to achieve
- true p-persistent CSMA (Carrier-Sense Multiple Access) in Packet operation.
- PK [“n”] Default: none
- Mode: All Host: PK
- Parameters:
- "n"   - a hex number used to access the PK-232's memory and I/O
- locations.
- PK (Peek/Poke) permits access to memory locations.      To use the PK command:
- • Set the memory address into the ADDRESS command.
- • Use the PK command without arguments to read that memory location.
- • Use PK with one argument O-$FF to write to that memory location.
- PK-232 RAM locations are $8000-$FFFF.  ROM begins at $0000.  This command is used
- primarily as a programmer's aid and is not needed for normal PK-232 use.
- PPersist ON/OFF Default: ON
- Mode: Packet Host: pp
- Parameters:
- ON The PK-232 uses p-persistent CSRA (Carrier Sense Multiple Access). 
- OFF The PX-232 uses DWAIT for TAPR-type 1-persistent CSMA.
- When PPERSIST is ON (default), the PK-232 uses the PERSIST and SLOTTIME
- parameters for p-persistent CSMA instead of the older DWAIT CSMA procedure.
- When your computer has queued data for transmission, the PK-232 monitors the DCD 
- signal from its modem.  When the channel clears, the PK-232 generates a random 
- number between 0 and 255.  If this number is less-than or equal to "PERSIST", 
- the PK-232 transmits all frames in its queue.  If the random number is greater 
- than "P", the PK-232 waits .01 * SLOTTIME seconds and repeats the attempt.
- PPERSIST can be used in both KISS and normal AX.25 operation.
**Description:**


---

### PACTor (PT for short) Immediate Command 
**Mode:** Command Host: Pt
**Description:**
Pactor is an immediate command that switches the PK-232 into the Pactor mode of operation.  This mode is an option. Pactor is a mode of data communication that combines some of the features of both AMTOR and packet.  The abbreviated command is PT.  It has both a linked mode called ARQ and a non-linked mode called unproto.  See Chapter 11 for details.

---

### PARity "n" Default: 3 (even)
**Mode:** All Host: PR
**Parameters:**
- “n”      — 0 to 3 selects a parity option from the table below.
- PARITY sets the PK-232's parity for RS-232 terminal according to the table below:
- 0 no parity 2 no parity
- 1 odd parity 3 even parity
- The parity bit, if present, is stripped automatically on input and is not checked
- in command and Converse Modes.  In Transparent Mode all eight bits (including
- parity) are transmitted.
- The change will not take effect until a RESTART is performed.  Be sure to change
- the computer or terminal to the same parity setting.
- PASs  "n” Default: $16 <CTRL-V>
- Mode: Packet, ASCII and Pactor Host:  PS
- Parameters:
- 0 to $7F (O to 127 decimal) specifies an ASCII character code.
- PASS selects the ASCII character used for the "pass' input editing commands.  The
- parameter “n” is the ASCII code for the character used to pass editing characters
- (default <CTRL-V>).  The PASS character signals that the following character is
- to be included in a packet Pactor or ASCII text string.
- PK-232 OPERATING MANUAL             COMMAND SUMMARY
- The rest of this page is blank
- PASSAll ON/OFF Default: OFF
- Mode: Packet Host: PK
- Parameters:
- ON -Your PK-232 will accept packets with valid or invalid  CRCS.
- OFF -Your PK-232 will accept packets with valid CRCs only.
- PASSALL turns off the PK-232's packet error-detecting mechanism and displays
- received packets with invalid CRCS.  PASSALL is normally turned OFF (default);
- which ensures that packet data la error-free by rejecting packets with invalid
- CRC fields.  When PASSALL is ON, packets are displayed, despite CRC errors.  The
- MHEARD logging is disabled since the call signs detected may be incorrect.
- PErsist “n" Default: 63
- Mode: Packet Host: PE
- Parameters:
- “n"  —  0 to 255 specifies the threshold for a random attempt to transmit.
- The PERSIST parameter works with the PPERSIST and SLOTTIME parameter to achieve
- true p-persistent CSMA (Carrier-Sense Multiple Access) in Packet operation.
- PK [“n”] Default: none
- Mode: All Host: PK
- Parameters:
- "n"   - a hex number used to access the PK-232's memory and I/O
- locations.
- PK (Peek/Poke) permits access to memory locations.      To use the PK command:
- • Set the memory address into the ADDRESS command.
- • Use the PK command without arguments to read that memory location.
- • Use PK with one argument O-$FF to write to that memory location.
- PK-232 RAM locations are $8000-$FFFF.  ROM begins at $0000.  This command is used
- primarily as a programmer's aid and is not needed for normal PK-232 use.
- PPersist ON/OFF Default: ON
- Mode: Packet Host: pp
- Parameters:
- ON The PK-232 uses p-persistent CSRA (Carrier Sense Multiple Access). 
- OFF The PX-232 uses DWAIT for TAPR-type 1-persistent CSMA.
- When PPERSIST is ON (default), the PK-232 uses the PERSIST and SLOTTIME
- parameters for p-persistent CSMA instead of the older DWAIT CSMA procedure.
- When your computer has queued data for transmission, the PK-232 monitors the DCD 
- signal from its modem.  When the channel clears, the PK-232 generates a random 
- number between 0 and 255.  If this number is less-than or equal to "PERSIST", 
- the PK-232 transmits all frames in its queue.  If the random number is greater 
- than "P", the PK-232 waits .01 * SLOTTIME seconds and repeats the attempt.
- PPERSIST can be used in both KISS and normal AX.25 operation.
**Description:**


---

### PASs  "n” Default: $16 <CTRL-V>
**Mode:** Packet, ASCII and Pactor Host:  PS
**Parameters:**
- 0 to $7F (O to 127 decimal) specifies an ASCII character code.
- PASS selects the ASCII character used for the "pass' input editing commands.  The
- parameter “n” is the ASCII code for the character used to pass editing characters
- (default <CTRL-V>).  The PASS character signals that the following character is
- to be included in a packet Pactor or ASCII text string.
- PK-232 OPERATING MANUAL             COMMAND SUMMARY
- The rest of this page is blank
- PASSAll ON/OFF Default: OFF
- Mode: Packet Host: PK
- Parameters:
- ON -Your PK-232 will accept packets with valid or invalid  CRCS.
- OFF -Your PK-232 will accept packets with valid CRCs only.
- PASSALL turns off the PK-232's packet error-detecting mechanism and displays
- received packets with invalid CRCS.  PASSALL is normally turned OFF (default);
- which ensures that packet data la error-free by rejecting packets with invalid
- CRC fields.  When PASSALL is ON, packets are displayed, despite CRC errors.  The
- MHEARD logging is disabled since the call signs detected may be incorrect.
- PErsist “n" Default: 63
- Mode: Packet Host: PE
- Parameters:
- “n"  —  0 to 255 specifies the threshold for a random attempt to transmit.
- The PERSIST parameter works with the PPERSIST and SLOTTIME parameter to achieve
- true p-persistent CSMA (Carrier-Sense Multiple Access) in Packet operation.
- PK [“n”] Default: none
- Mode: All Host: PK
- Parameters:
- "n"   - a hex number used to access the PK-232's memory and I/O
- locations.
- PK (Peek/Poke) permits access to memory locations.      To use the PK command:
- • Set the memory address into the ADDRESS command.
- • Use the PK command without arguments to read that memory location.
- • Use PK with one argument O-$FF to write to that memory location.
- PK-232 RAM locations are $8000-$FFFF.  ROM begins at $0000.  This command is used
- primarily as a programmer's aid and is not needed for normal PK-232 use.
- PPersist ON/OFF Default: ON
- Mode: Packet Host: pp
- Parameters:
- ON The PK-232 uses p-persistent CSRA (Carrier Sense Multiple Access). 
- OFF The PX-232 uses DWAIT for TAPR-type 1-persistent CSMA.
- When PPERSIST is ON (default), the PK-232 uses the PERSIST and SLOTTIME
- parameters for p-persistent CSMA instead of the older DWAIT CSMA procedure.
- When your computer has queued data for transmission, the PK-232 monitors the DCD 
- signal from its modem.  When the channel clears, the PK-232 generates a random 
- number between 0 and 255.  If this number is less-than or equal to "PERSIST", 
- the PK-232 transmits all frames in its queue.  If the random number is greater 
- than "P", the PK-232 waits .01 * SLOTTIME seconds and repeats the attempt.
- PPERSIST can be used in both KISS and normal AX.25 operation.
**Description:**


---

### PASSAll ON/OFF Default: OFF
**Mode:** Packet Host: PK
**Parameters:**
- ON -Your PK-232 will accept packets with valid or invalid  CRCS.
- OFF -Your PK-232 will accept packets with valid CRCs only.
- PASSALL turns off the PK-232's packet error-detecting mechanism and displays
- received packets with invalid CRCS.  PASSALL is normally turned OFF (default);
- which ensures that packet data la error-free by rejecting packets with invalid
- CRC fields.  When PASSALL is ON, packets are displayed, despite CRC errors.  The
- MHEARD logging is disabled since the call signs detected may be incorrect.
- PErsist “n" Default: 63
- Mode: Packet Host: PE
- Parameters:
- “n"  —  0 to 255 specifies the threshold for a random attempt to transmit.
- The PERSIST parameter works with the PPERSIST and SLOTTIME parameter to achieve
- true p-persistent CSMA (Carrier-Sense Multiple Access) in Packet operation.
- PK [“n”] Default: none
- Mode: All Host: PK
- Parameters:
- "n"   - a hex number used to access the PK-232's memory and I/O
- locations.
- PK (Peek/Poke) permits access to memory locations.      To use the PK command:
- • Set the memory address into the ADDRESS command.
- • Use the PK command without arguments to read that memory location.
- • Use PK with one argument O-$FF to write to that memory location.
- PK-232 RAM locations are $8000-$FFFF.  ROM begins at $0000.  This command is used
- primarily as a programmer's aid and is not needed for normal PK-232 use.
- PPersist ON/OFF Default: ON
- Mode: Packet Host: pp
- Parameters:
- ON The PK-232 uses p-persistent CSRA (Carrier Sense Multiple Access). 
- OFF The PX-232 uses DWAIT for TAPR-type 1-persistent CSMA.
- When PPERSIST is ON (default), the PK-232 uses the PERSIST and SLOTTIME
- parameters for p-persistent CSMA instead of the older DWAIT CSMA procedure.
- When your computer has queued data for transmission, the PK-232 monitors the DCD 
- signal from its modem.  When the channel clears, the PK-232 generates a random 
- number between 0 and 255.  If this number is less-than or equal to "PERSIST", 
- the PK-232 transmits all frames in its queue.  If the random number is greater 
- than "P", the PK-232 waits .01 * SLOTTIME seconds and repeats the attempt.
- PPERSIST can be used in both KISS and normal AX.25 operation.
**Description:**


---

### PErsist “n" Default: 63
**Mode:** Packet Host: PE
**Parameters:**
- “n"  —  0 to 255 specifies the threshold for a random attempt to transmit.
- The PERSIST parameter works with the PPERSIST and SLOTTIME parameter to achieve
- true p-persistent CSMA (Carrier-Sense Multiple Access) in Packet operation.
- PK [“n”] Default: none
- Mode: All Host: PK
- Parameters:
- "n"   - a hex number used to access the PK-232's memory and I/O
- locations.
- PK (Peek/Poke) permits access to memory locations.      To use the PK command:
- • Set the memory address into the ADDRESS command.
- • Use the PK command without arguments to read that memory location.
- • Use PK with one argument O-$FF to write to that memory location.
- PK-232 RAM locations are $8000-$FFFF.  ROM begins at $0000.  This command is used
- primarily as a programmer's aid and is not needed for normal PK-232 use.
- PPersist ON/OFF Default: ON
- Mode: Packet Host: pp
- Parameters:
- ON The PK-232 uses p-persistent CSRA (Carrier Sense Multiple Access). 
- OFF The PX-232 uses DWAIT for TAPR-type 1-persistent CSMA.
- When PPERSIST is ON (default), the PK-232 uses the PERSIST and SLOTTIME
- parameters for p-persistent CSMA instead of the older DWAIT CSMA procedure.
- When your computer has queued data for transmission, the PK-232 monitors the DCD 
- signal from its modem.  When the channel clears, the PK-232 generates a random 
- number between 0 and 255.  If this number is less-than or equal to "PERSIST", 
- the PK-232 transmits all frames in its queue.  If the random number is greater 
- than "P", the PK-232 waits .01 * SLOTTIME seconds and repeats the attempt.
- PPERSIST can be used in both KISS and normal AX.25 operation.
**Description:**


---

### PK [“n”] Default: none
**Mode:** All Host: PK
**Parameters:**
- "n"   - a hex number used to access the PK-232's memory and I/O
- locations.
- PK (Peek/Poke) permits access to memory locations.      To use the PK command:
- • Set the memory address into the ADDRESS command.
- • Use the PK command without arguments to read that memory location.
- • Use PK with one argument O-$FF to write to that memory location.
- PK-232 RAM locations are $8000-$FFFF.  ROM begins at $0000.  This command is used
- primarily as a programmer's aid and is not needed for normal PK-232 use.
- PPersist ON/OFF Default: ON
- Mode: Packet Host: pp
- Parameters:
- ON The PK-232 uses p-persistent CSRA (Carrier Sense Multiple Access). 
- OFF The PX-232 uses DWAIT for TAPR-type 1-persistent CSMA.
- When PPERSIST is ON (default), the PK-232 uses the PERSIST and SLOTTIME
- parameters for p-persistent CSMA instead of the older DWAIT CSMA procedure.
- When your computer has queued data for transmission, the PK-232 monitors the DCD 
- signal from its modem.  When the channel clears, the PK-232 generates a random 
- number between 0 and 255.  If this number is less-than or equal to "PERSIST", 
- the PK-232 transmits all frames in its queue.  If the random number is greater 
- than "P", the PK-232 waits .01 * SLOTTIME seconds and repeats the attempt.
- PPERSIST can be used in both KISS and normal AX.25 operation.
**Description:**


---

### PPersist ON/OFF Default: ON
**Mode:** Packet Host: pp
**Parameters:**
- ON The PK-232 uses p-persistent CSRA (Carrier Sense Multiple Access). 
- OFF The PX-232 uses DWAIT for TAPR-type 1-persistent CSMA.
- When PPERSIST is ON (default), the PK-232 uses the PERSIST and SLOTTIME
- parameters for p-persistent CSMA instead of the older DWAIT CSMA procedure.
- When your computer has queued data for transmission, the PK-232 monitors the DCD 
- signal from its modem.  When the channel clears, the PK-232 generates a random 
- number between 0 and 255.  If this number is less-than or equal to "PERSIST", 
- the PK-232 transmits all frames in its queue.  If the random number is greater 
- than "P", the PK-232 waits .01 * SLOTTIME seconds and repeats the attempt.
- PPERSIST can be used in both KISS and normal AX.25 operation.
**Description:**


---

### PRCon  ON|OFF                                               Default: OFF
**Mode:**  All                                                  Host: PC
**Parameters:**
- ON   -    A parallel printer is connected to the PK-232 via the special cable.
- OFF  -    There is no parallel printer connected, or it is not currently in use.
**Description:**
PRCON tells the PK-232 whether or not a parallel printer is connected via the  optional "Y" facsimile cable.  When PRCON is ON, the mode and status LED's are  disabled, and do not perform their proper function.  Some LED's may remain lit,  and others may flash randomly.  When you are through printing, disconnect the  printer, turn PRCON OFF and the status LED's will return to normal operation. _______________________________________________________________________________

---

### PRFax  ON|OFF                                               Default: ON
**Mode:**  FAX                                                  Host: PF
**Parameters:**
- ON   -    FAX graphics are sent to the parallel printer if PRCON is on.
- OFF  -    FAX graphics are sent to the RS-232 serial port.
**Description:**
PRFAX should be ON if a parallel printer is connected to the PK-232 and in use  for printing facsimile pictures.  If you wish to save the facsimile graphic data  to disk for later retrieval, simply turn PRFAX OFF and set AWLEN to 8 to allow  8-bit data to be sent to the serial port. _______________________________________________________________________________

---

### PROut  ON|OFF                                               Default: OFF
**Mode:**  All                                                  Host: PO
**Parameters:**
- ON   -    All characters are sent to the parallel printer if PRCON is also ON.
- OFF  -    All text and data is sent to the terminal through the serial port. 
**Description:**
This command is useful for any mode where the user wants to get a printed copy  of what is being received by the PK-232.  It is especially desirable for those  using a terminal with the PK-232 who otherwise can not get hard copy of received  text.  It is also handy if you wish to use your computer for another purpose,  and still monitor channel activity.

_______________________________________________________________________________

---

### PRType  "n"                                            Default: 2 (Epson)
**Mode:**  FAX                                             Host: PY
**Parameters:**
- "n"   -   0 to 255, specifying a code for the type of dot graphics sequences 
- used by your printer.
**Description:**
The following is a list of the different printer graphics types the PK-232  supports.  Most of these types are broken up by manufacturers, however EPSON and  IBM are the most popular and are supported by many printer manufacturers not  shown in the list below. If you are unsure about which type of graphics printer you have, check your  printer's manual and locate a graphics command that matches one from the  GRAPHICS ON section of the table below.  If you find that your printer supports  the "CHR$(27) K n1 n2" then try the EPSON (default) or IBM graphics formats  before any of the others listed.  These are the most widespread graphics formats  in use that it is very likely your printer supports at least one of them. PRTYPE    Printer                       GRAPHICS ON Sequence 0         Epson                         CHR$(27) K n1 n2 4         IBM                           CHR$(27) K n1 n2 8         Radio Shack (Tandy)           CHR$(18) 12        Apple (G)                     CHR$(27) G n n n n 16        Apple (S)                     CHR$(27) S n n n n 20        old Okidata                   CHR$(3) 24        Okidata                       CHR$(3) 28        Gemini 10, 15                 CHR$(27) K n1 n2 32        Star Micronics                CHR$(27) K n1 n2 36        GX-100, Gorilla               CHR$(8) 40        Texas Instruments             CHR$(27) K n1 n2 44        Genicom                       CHR$(27) K n1 n2 48        Miscellaneous (HP ThinkJet) 52        Citizen                       CHR$(27) K n1 n2 56        NEC                           CHR$(27) > CHR$(27) M CHR$(27) S0960 60        Anadex                        CHR$(28) Unsupported PRTYPE settings are treated as PRTYPE 0. Notice that the PRTYPEs are assigned in groups of four.  This is done to handle  the different carriage widths used (standard 8-1/2" and wide 13") and also the  number of data bits the printer can accept (7 or 8).  Add the following number  to each of the above PRTYPEs to customize the PK-232 for your printer. +0:     7-bit graphics data, standard (8-1/2" paper) printer carriage +1:     7-bit graphics data, wide (13" paper) printer carriage +2:     8-bit graphics data, standard (8-1/2" paper) printer carriage +3:     8-bit graphics data, wide (13" paper) printer carriage As an example, the default PRTYPE setting of 2 was chosen because most printers  are standard width (8-1/2" paper) and will handle the Epson 8-bit graphics  format.  Thus the Epson PRTYPE of 0 was chosen from the table above, and the  quantity +2 was added to select the standard printer carriage and 8-bit data. If you have a wide carriage Epson printer (and wide paper of course) you would  have wanted to add the quantity +3 to the Epson PRTYPE listed in the table.

---

### PT200 ON/OFF Default: ON
**Mode:** Pactor Host: PB
**Description:**
Pactor uses an adaptive data rate selection scheme.  The normal data rate is 100 baud.  If conditions permit, the data rate will be shifted to 200 baud automatically.  If the error rate becomes too high at 200 baud the data rate will automatically be reduced to 100 baud.  There can be conditions where the data rate is frequently shifting, causing a loss in the actual information data rate. The command PT200 when off, will prevent the data rate from automatically changing to 200 baud.  When PT200 is ON (default), the PK-232 will allow automatic data rate selection.

---

### PTConn [!]aaaa(aa)                                      
**Mode:** Pactor  Host: PG
**Parameters:**
- aaaa(aa) is the call sign of the Pactor station to be called
- PTConn is an immediate command that starts the Pactor connect protocol.  To start
- a Pactor connect, type "PIC' followed by the other station's call sign:
- Example: PTC N7ML <Enter>
- As soon as the <CR> is typed, the PK-232 will begin keying your transmitter on
- and off with the Pactor connect sequence.
- If you are connecting with a station via long path, i.e. more than half way
- around the world, use an exclamation mark before the callsign:
- Example:  PTC !N7ML <enter>
- This changes the Pactor timing to allow for the extended radio propagation time.
- PTHUFF “n” Default: 0
- Mode: Pactor Host: pH
- "n"   —    0 to 3, specifying a type of compression that may be used in Pactor.
- To enhance the effective data rate in Pactor, a data compression scheme called
- may be automatically enabled.
- 0    - is no compression (default).
- 1    - is Huffman encoding.
- 2,3 - presently not implemented but reserved for future use.
- Instead of using the normal 8 bit ASCII representation of a character, Huffman
- encoding assigns each character a code that may be as few as 2 bits for the most
- used characters to as long as 15 bits for the least used characters.  For English
- (and most other) languages, the use of Huffman compression results in a smaller
- number of bits necessary for a given message.  For messages consisting of non-
- text information such as computer programs, Huffman compression would need more
- bits than ASCII and would be less efficient.
- If PTH is set off, Pactor will never use Huffman compression.  When PTH is set
- on, Huffman compression will be used if it is more effective.  Do not use Huffman
- compression with binary file transfers as it only works with 7 bit data.
- PTList Immediate Command
- Mode: Pactor Host: PN
- PTList is an immediate command that switches your PK-232 into the Pactor listen
- mode.
- You can usually monitor a Pactor contact between two connected stations using the
- Pactor listen mode.  Since your station is not part of the error free link, if
- the CRC check does not produce a correct check sum, nothing will be displayed.
- If the receiving station requests a repeat, and you have copied the packet, it
- will not print twice.
- PTOver “n” Default: <CTRL-Z> ($lA) 
- Mode: Pactor Host: PV
- “n”  — A hexadecimal value from $00 to $7F used to select the change-over
- character used in linked Pactor.  The default is <CTRL-Z>.
- PTOver is the character, conventionally <CTRL-Z>, that is used to change the
- direction of data transmission in a linked Pactor operation.  When you are
- finished transmitting information and you are ready to receive information from
- the other station, use the PTOver character.  Also see AChg.
- PTSend – “n, x” Default: 1,2
- Mode: Pactor Host: PD
- Parameters:
- ”n” - 1 or 2 selects the transmit baud rate.
- - 1 to 5 selects the number of times the data is repeated.
- PTS “n, x” initiates an unproto Pactor transmission.  To end the transmission,
- type <ctrl-D>.
- “n”
- 1 selects 100 baud,
- 2 selects 200 baud.
- In order to increase the probability of correct transmission, the unproto
- Pactor transmission sends the message data a selected number of times.
- The parameter x sets the number of times the data is sent.
- Example:
- PTS 1 3 <Enter> would start a 100 baud unproto transmission with the
- message data sent three times.
- The transmission may be started using the default, 100 baud, two repeats, by
- typing "PTS" without “n x.”
- Example:
- PTS <Enter>
- PK-232 OPERATING MANUAL                          COMMAND SUMMARY
- The rest of this page is blank
**Description:**


---

### PTHUFF “n” Default: 0
**Mode:** Pactor Host: pH
**Description:**
"n"   —    0 to 3, specifying a type of compression that may be used in Pactor. To enhance the effective data rate in Pactor, a data compression scheme called may be automatically enabled. 0    - is no compression (default). 1    - is Huffman encoding. 2,3 - presently not implemented but reserved for future use. Instead of using the normal 8 bit ASCII representation of a character, Huffman encoding assigns each character a code that may be as few as 2 bits for the most used characters to as long as 15 bits for the least used characters.  For English (and most other) languages, the use of Huffman compression results in a smaller number of bits necessary for a given message.  For messages consisting of non- text information such as computer programs, Huffman compression would need more bits than ASCII and would be less efficient. If PTH is set off, Pactor will never use Huffman compression.  When PTH is set on, Huffman compression will be used if it is more effective.  Do not use Huffman compression with binary file transfers as it only works with 7 bit data.

---

### PTList Immediate Command
**Mode:** Pactor Host: PN
**Description:**
PTList is an immediate command that switches your PK-232 into the Pactor listen mode. You can usually monitor a Pactor contact between two connected stations using the Pactor listen mode.  Since your station is not part of the error free link, if the CRC check does not produce a correct check sum, nothing will be displayed. If the receiving station requests a repeat, and you have copied the packet, it will not print twice.

---

### PTOver “n” Default: <CTRL-Z> ($lA) 
**Mode:** Pactor Host: PV
**Description:**
“n”  — A hexadecimal value from $00 to $7F used to select the change-over character used in linked Pactor.  The default is <CTRL-Z>. PTOver is the character, conventionally <CTRL-Z>, that is used to change the direction of data transmission in a linked Pactor operation.  When you are finished transmitting information and you are ready to receive information from the other station, use the PTOver character.  Also see AChg.

---

### PTSend – “n, x” Default: 1,2
**Mode:** Pactor Host: PD
**Parameters:**
- ”n” - 1 or 2 selects the transmit baud rate.
- - 1 to 5 selects the number of times the data is repeated.
- PTS “n, x” initiates an unproto Pactor transmission.  To end the transmission,
- type <ctrl-D>.
- “n”
- 1 selects 100 baud,
- 2 selects 200 baud.
- In order to increase the probability of correct transmission, the unproto
- Pactor transmission sends the message data a selected number of times.
- The parameter x sets the number of times the data is sent.
- Example:
- PTS 1 3 <Enter> would start a 100 baud unproto transmission with the
- message data sent three times.
- The transmission may be started using the default, 100 baud, two repeats, by
- typing "PTS" without “n x.”
- Example:
- PTS <Enter>
- PK-232 OPERATING MANUAL                          COMMAND SUMMARY
- The rest of this page is blank
**Description:**


---

### RAWhdlc ON|OFF                                              Default: OFF
**Mode:** Packet                                                Host: RW
**Parameters:**
- ON   -    The PK-232 operates in a raw HDLC packet mode when HOST is ON.
- OFF  -    The PK-232 operates in standard AX.25.
**Description:**
The RAWHDLC command enables the PK-232 to bypass the AX.25 packet implementation  and communicate directly with the hardware HDLC (Z8530). HOST mode must be ON to communicate with the PK-232 in the RAW HDLC mode. See Timewave's PK-232 Technical Manual for full information on Raw HDLC Mode. _______________________________________________________________________________

---

### RBaud "n"                                    Default:  45 bauds (60 WPM)
**Mode:** Baudot RTTY                            Host: RB
**Parameters:**
- "n"  -    Specifies the Baudot data rate in bauds from the PK-232 to the radio.
**Description:**
RBAUD sets the radio ("on-air") baud rate only in the Baudot operating mode.   This value has no relationship to your computer or terminal program's baud rate. Available Baudot data rates include 45, 50, 57, 75, 100, 110, 150, 200 and 300  bauds (60, 66, 75, 100, 132, 145, 198, 264 and 396 WPM). You may use RBAUD UP (RB U) to go to the next highest Baudot speed or RBAUD          DOWN (RB D) to go to the next lowest Baudot speed. _______________________________________________________________________________

---

### Rcve                                                   Immediate Command
**Mode:** Baudot/ASCII/AMTOR/FAX/Morse                     Host: RC
**Description:**
_______________________________________________________________________________  RCVE is an immediate command, used in Morse, Baudot, ASCII, ARQ, FEC and FAX  modes to switch your PK-232 from transmit to receive. o    You must return to the Command Mode to use the RCVE command.  _______________________________________________________________________________

---

### RECeive "n"                                       Default: $04 <CTRL-D>
**Mode:** Baudot/ASCII/Morse/AMTOR/FAX                Host: RE
**Parameters:**
- "n"  -    0 to $7F (0 to 127 decimal) specifies an ASCII character code.
**Description:**
Parameter "n" is the numeric ASCII code for the character you'll use when you  want the PK-232 to return to receive. The RECEIVE command allows you to insert a character (default <CTRL-D>) in your  typed text that will cause the PK-232 to return to receive after all the text  has been transmitted.

_______________________________________________________________________________

---

### REDispla "n"                                      Default: $12 <CTRL-R>
**Mode:** All                                         Host: RD
**Parameters:**
- "n"  -    0 to $7F (0 to 127 decimal) specifies an ASCII character code.
**Description:**
REDISPLA changes the redisplay-line input editing character. Parameter "n" is the numeric ASCII code for the character you'll use when you  want to re-display the current input line. Type the REDISPLA character (default <CTRL-R>) to re-display a command or text  line you've just typed.  This can be helpful when editing a line especially if  your terminal does not support <BACKSPACE>.  It can also be used in Packet to  display a packet that might have been received while you were typing.  A  <BACKSLASH> is appended to old line, and the corrected line is shown below it.  _______________________________________________________________________________

---

### RELink ON|OFF                                               Default: OFF
**Mode:** Packet                                                Host: RL
**Parameters:**
- ON   -    The PK-232 will try to automatically reconnect the distant station 
- after the link has timed out on retries.
- OFF  -    The PK-232 will not attempt to re-establish the failed link (default).
**Description:**
Set RELINK ON if you want the PK-232 to automatically try to reconnect to a  distant packet station if the link fails. _______________________________________________________________________________

---

### RESET                                                  Immediate Command
**Mode:** Command                                          Host: RS
**Description:**
_______________________________________________________________________________ RESET is an immediate command that resets all parameters to PK-232's PROM  default settings and reinitializes the PK-232.  All personalized parameters,  monitor lists and MailDrop messages will be lost. _______________________________________________________________________________

---

### RESptime "n"                                      Default: 0 (000 msec.)
**Mode:** Packet                                      Host: RP
**Parameters:**
- "n"  -    0 to 250 specifies 100-millisecond intervals.
**Description:**
RESPTIME adds a minimum delay before your PK-232 sends acknowledgment packets.   This delay may run concurrently with the default wait time set by DWAIT and any  random wait in effect. During a file transfer, RESPTIME can help avoid data/ack collisions caused by  the sending stations TNC pausing briefly between transmitted data frames.

---

### RESTART Immediate Command
**Mode:** Command Host:   RT
**Description:**
RESTART is an immediate command that reinitializes the PK-232 while retaining the user's settings.  The effect of the RESTART command is the same as turning the PK-232 OFF, then ON again. RESTART does not reset the values in bbRAM.   See the RESET command.

---

### REtry “n” Default: 10
**Mode:** Packet Host: RY
**Parameters:**
- “n”       0 to 15 specifies the maximum number of packet-retries.
- The AX.25 protocol uses the retransmission of frames that have not been
- acknowledged as a means to insure that ALL transmitted frames are received.  The
- number of retries that the PK-232 will attempt is set by the RETRY command
- (default 10).  If the number of retries is exceeded, the packet link may be lost.
- RFec ON/OFF Default: ON
- Mode: AMTOR Host: RF
- Parameters:
- ON -Mode B (FEC) signals are displayed in AMTOR Standby (default).
- OFF -Mode B (FEC) signals are not displayed in AMTOR Standby.
- Turn the RFEC command OFF to prevent the reception and display of all FEC signals
- received while in AMTOR Standby.
- RFRame  ON/OFF Default: OFF
- Mode:     Baudot and ASCII RTTY Host: RG
- Parameters:
- ON Check received Baudot and ASCII characters for framing errors.  
- OFF Print received Baudot and ASCII characters regardless of errors.
- When RFRAME is OFF (default), Baudot and ASCII modes operate as always, that is
- characters are copied based on the presence of the DCD signal.
- When RFRAME is ON, the PK-232 checks received Baudot and ASCII characters for
- framing errors.  A framing error on a character in an asynchronous mode (such as
- Baudot and ASCII) occurs when the bit in the stop position is detected to be the
- wrong polarity (the polarity of the start bit is supposed to be space or 0, while
- the stop bit is supposed to be mark or 1).  The unit stops copying characters
- when 4 out of the last 12 characters had framing errors.  Copy resumes when the
- most recent 12 characters are error-free.  This should significantly reduce the
- copying of garbage characters when  no signals are present.  When RFRAME is ON,
- characters are copied based on the recent history of framing errors.
- RXRev   ON/OFF Default: OFF
- Mode:   Baudot and ASCII RTTY/AMTOR Host: RX
- Parameters:
- ON — Received data polarity is reversed (mark-space reversal).
- OFF — Received data polarity is normal.
- Use the RXREV Command to invert the polarity of the data demodulated from the
- received mark and space tones.
- In some cases, you may be trying to copy a station that's transmitting "upside
- down" although it is receiving your signals correctly.  This is especially true
- when listening to signals in the Short Wave bands.  Set RXREV ON to reverse the
- data sense of received signals.
- RXREV operates only at RBAUD and ABAUD speeds up to 150 baud.
- PK-232 OPERATING MANUAL                                    
- COMMAND SUMMARY
- The rest of this page is blank
- SAmple “n” Immediate Command
- Mode: Command Host: SA
- “n"     —         20 to 255 specifies the sampling rate in baud.
- This is a new operating mode with the Summer 1991 PK-232 firmware release for
- advanced users interested in decoding unknown synchronous data transmissions.
- SAMPLE is similar to the SBIT and 6BIT modes, but operates on synchronous data,
- whereas 5B1T and 6BIT are used on signals known to be asynchronous.
- SAMPLE syncs up on any regularly-paced data transmission and samples the data
- once per bit, packaging the data in groups and sending the groups to the user for
- further analysis.  The user can use SAMPLE to capture data bits from a
- synchronous transmission, such as FEC, TDM or an 'unknown model' not identified
- by the SIGNAL command.  The transmission is actually sampled several times per
- data bit.  The PK-232 does a majority vote an the last few samples to represent
- the value of the data bit.
- One use for the SAMPLE command is to record the output to a disk file, then write
- a program to analyze the results for synchronous/asynchronous, bit sync patterns,
- data decoding, etc.
- SAMPLE data is captured in 6-bit units; the order of bit reception is MSB first,
- LSB last.  The TNC sends the data unit to the user with a constant of hex 30
- added to each unit, the same as the 6BIT command.  The 6-bit unit is a compromise
- between hexadecimal and 8-bit binary output, The 6-bit unit yields shorter disk
- files than 4-bit hexadecimal characters, but encounters no interference from
- terminal communications programs and the TNC's Converse and Command modes.  The
- 6-bit unit's range of $30-6F falls within the printable ASCII range, allowing the
- TNC to insert end-of-line carriage returns that can be ignored by the user's
- analysis software.
- To use SAMPLE, set ACRDISP to a non-zero value such as 77.  This will break up
- the recorded disk file into lines.  Tune in the signal, set WIDESHFT ON or OFF as
- needed, and get the transmission rate from the SIGNAL command.  Now type "SAMPLE
- (rate)".  As an example, SIGNAL may identify a transmission as 96 baud TDM; in
- this case type the following:
- SAMPLE 96 <Carriage Return>
- Now begin the capture to a disk file with the terminal program. At the end of the
- session, edit the disk file and remove any TNC commands that were echoed before
- or after the received data.
- Occasionally SIGNAL will identify a Baudot transmission at a rate that SAMPLE
- cannot sync up on.  This would happen if the Baudot signal had a stop bit
- duration 1.5 times the data bit duration.  In this case, SAMPLE at twice the baud
- rate and compensate for the doubled data bits in the analysis software. Note that
- it might be more useful to let the TNC do the start/stop bit work by using the
- 5BIT command rather than SAMPLE.  5BIT uses RBAUD, and adds a constant of hex 40
- to each 5-bit character received.
- Note: RXREV does affect the sense of the SAMPLE data.
- RXREV should however not be changed while capturing data.
**Description:**


---

### RFec ON/OFF Default: ON
**Mode:** AMTOR Host: RF
**Parameters:**
- ON -Mode B (FEC) signals are displayed in AMTOR Standby (default).
- OFF -Mode B (FEC) signals are not displayed in AMTOR Standby.
- Turn the RFEC command OFF to prevent the reception and display of all FEC signals
- received while in AMTOR Standby.
- RFRame  ON/OFF Default: OFF
- Mode:     Baudot and ASCII RTTY Host: RG
- Parameters:
- ON Check received Baudot and ASCII characters for framing errors.  
- OFF Print received Baudot and ASCII characters regardless of errors.
- When RFRAME is OFF (default), Baudot and ASCII modes operate as always, that is
- characters are copied based on the presence of the DCD signal.
- When RFRAME is ON, the PK-232 checks received Baudot and ASCII characters for
- framing errors.  A framing error on a character in an asynchronous mode (such as
- Baudot and ASCII) occurs when the bit in the stop position is detected to be the
- wrong polarity (the polarity of the start bit is supposed to be space or 0, while
- the stop bit is supposed to be mark or 1).  The unit stops copying characters
- when 4 out of the last 12 characters had framing errors.  Copy resumes when the
- most recent 12 characters are error-free.  This should significantly reduce the
- copying of garbage characters when  no signals are present.  When RFRAME is ON,
- characters are copied based on the recent history of framing errors.
- RXRev   ON/OFF Default: OFF
- Mode:   Baudot and ASCII RTTY/AMTOR Host: RX
- Parameters:
- ON — Received data polarity is reversed (mark-space reversal).
- OFF — Received data polarity is normal.
- Use the RXREV Command to invert the polarity of the data demodulated from the
- received mark and space tones.
- In some cases, you may be trying to copy a station that's transmitting "upside
- down" although it is receiving your signals correctly.  This is especially true
- when listening to signals in the Short Wave bands.  Set RXREV ON to reverse the
- data sense of received signals.
- RXREV operates only at RBAUD and ABAUD speeds up to 150 baud.
- PK-232 OPERATING MANUAL                                    
- COMMAND SUMMARY
- The rest of this page is blank
- SAmple “n” Immediate Command
- Mode: Command Host: SA
- “n"     —         20 to 255 specifies the sampling rate in baud.
- This is a new operating mode with the Summer 1991 PK-232 firmware release for
- advanced users interested in decoding unknown synchronous data transmissions.
- SAMPLE is similar to the SBIT and 6BIT modes, but operates on synchronous data,
- whereas 5B1T and 6BIT are used on signals known to be asynchronous.
- SAMPLE syncs up on any regularly-paced data transmission and samples the data
- once per bit, packaging the data in groups and sending the groups to the user for
- further analysis.  The user can use SAMPLE to capture data bits from a
- synchronous transmission, such as FEC, TDM or an 'unknown model' not identified
- by the SIGNAL command.  The transmission is actually sampled several times per
- data bit.  The PK-232 does a majority vote an the last few samples to represent
- the value of the data bit.
- One use for the SAMPLE command is to record the output to a disk file, then write
- a program to analyze the results for synchronous/asynchronous, bit sync patterns,
- data decoding, etc.
- SAMPLE data is captured in 6-bit units; the order of bit reception is MSB first,
- LSB last.  The TNC sends the data unit to the user with a constant of hex 30
- added to each unit, the same as the 6BIT command.  The 6-bit unit is a compromise
- between hexadecimal and 8-bit binary output, The 6-bit unit yields shorter disk
- files than 4-bit hexadecimal characters, but encounters no interference from
- terminal communications programs and the TNC's Converse and Command modes.  The
- 6-bit unit's range of $30-6F falls within the printable ASCII range, allowing the
- TNC to insert end-of-line carriage returns that can be ignored by the user's
- analysis software.
- To use SAMPLE, set ACRDISP to a non-zero value such as 77.  This will break up
- the recorded disk file into lines.  Tune in the signal, set WIDESHFT ON or OFF as
- needed, and get the transmission rate from the SIGNAL command.  Now type "SAMPLE
- (rate)".  As an example, SIGNAL may identify a transmission as 96 baud TDM; in
- this case type the following:
- SAMPLE 96 <Carriage Return>
- Now begin the capture to a disk file with the terminal program. At the end of the
- session, edit the disk file and remove any TNC commands that were echoed before
- or after the received data.
- Occasionally SIGNAL will identify a Baudot transmission at a rate that SAMPLE
- cannot sync up on.  This would happen if the Baudot signal had a stop bit
- duration 1.5 times the data bit duration.  In this case, SAMPLE at twice the baud
- rate and compensate for the doubled data bits in the analysis software. Note that
- it might be more useful to let the TNC do the start/stop bit work by using the
- 5BIT command rather than SAMPLE.  5BIT uses RBAUD, and adds a constant of hex 40
- to each 5-bit character received.
- Note: RXREV does affect the sense of the SAMPLE data.
- RXREV should however not be changed while capturing data.
**Description:**


---

### RFRame  ON/OFF Default: OFF
**Mode:**     Baudot and ASCII RTTY Host: RG
**Parameters:**
- ON Check received Baudot and ASCII characters for framing errors.  
- OFF Print received Baudot and ASCII characters regardless of errors.
- When RFRAME is OFF (default), Baudot and ASCII modes operate as always, that is
- characters are copied based on the presence of the DCD signal.
- When RFRAME is ON, the PK-232 checks received Baudot and ASCII characters for
- framing errors.  A framing error on a character in an asynchronous mode (such as
- Baudot and ASCII) occurs when the bit in the stop position is detected to be the
- wrong polarity (the polarity of the start bit is supposed to be space or 0, while
- the stop bit is supposed to be mark or 1).  The unit stops copying characters
- when 4 out of the last 12 characters had framing errors.  Copy resumes when the
- most recent 12 characters are error-free.  This should significantly reduce the
- copying of garbage characters when  no signals are present.  When RFRAME is ON,
- characters are copied based on the recent history of framing errors.
- RXRev   ON/OFF Default: OFF
- Mode:   Baudot and ASCII RTTY/AMTOR Host: RX
- Parameters:
- ON — Received data polarity is reversed (mark-space reversal).
- OFF — Received data polarity is normal.
- Use the RXREV Command to invert the polarity of the data demodulated from the
- received mark and space tones.
- In some cases, you may be trying to copy a station that's transmitting "upside
- down" although it is receiving your signals correctly.  This is especially true
- when listening to signals in the Short Wave bands.  Set RXREV ON to reverse the
- data sense of received signals.
- RXREV operates only at RBAUD and ABAUD speeds up to 150 baud.
- PK-232 OPERATING MANUAL                                    
- COMMAND SUMMARY
- The rest of this page is blank
- SAmple “n” Immediate Command
- Mode: Command Host: SA
- “n"     —         20 to 255 specifies the sampling rate in baud.
- This is a new operating mode with the Summer 1991 PK-232 firmware release for
- advanced users interested in decoding unknown synchronous data transmissions.
- SAMPLE is similar to the SBIT and 6BIT modes, but operates on synchronous data,
- whereas 5B1T and 6BIT are used on signals known to be asynchronous.
- SAMPLE syncs up on any regularly-paced data transmission and samples the data
- once per bit, packaging the data in groups and sending the groups to the user for
- further analysis.  The user can use SAMPLE to capture data bits from a
- synchronous transmission, such as FEC, TDM or an 'unknown model' not identified
- by the SIGNAL command.  The transmission is actually sampled several times per
- data bit.  The PK-232 does a majority vote an the last few samples to represent
- the value of the data bit.
- One use for the SAMPLE command is to record the output to a disk file, then write
- a program to analyze the results for synchronous/asynchronous, bit sync patterns,
- data decoding, etc.
- SAMPLE data is captured in 6-bit units; the order of bit reception is MSB first,
- LSB last.  The TNC sends the data unit to the user with a constant of hex 30
- added to each unit, the same as the 6BIT command.  The 6-bit unit is a compromise
- between hexadecimal and 8-bit binary output, The 6-bit unit yields shorter disk
- files than 4-bit hexadecimal characters, but encounters no interference from
- terminal communications programs and the TNC's Converse and Command modes.  The
- 6-bit unit's range of $30-6F falls within the printable ASCII range, allowing the
- TNC to insert end-of-line carriage returns that can be ignored by the user's
- analysis software.
- To use SAMPLE, set ACRDISP to a non-zero value such as 77.  This will break up
- the recorded disk file into lines.  Tune in the signal, set WIDESHFT ON or OFF as
- needed, and get the transmission rate from the SIGNAL command.  Now type "SAMPLE
- (rate)".  As an example, SIGNAL may identify a transmission as 96 baud TDM; in
- this case type the following:
- SAMPLE 96 <Carriage Return>
- Now begin the capture to a disk file with the terminal program. At the end of the
- session, edit the disk file and remove any TNC commands that were echoed before
- or after the received data.
- Occasionally SIGNAL will identify a Baudot transmission at a rate that SAMPLE
- cannot sync up on.  This would happen if the Baudot signal had a stop bit
- duration 1.5 times the data bit duration.  In this case, SAMPLE at twice the baud
- rate and compensate for the doubled data bits in the analysis software. Note that
- it might be more useful to let the TNC do the start/stop bit work by using the
- 5BIT command rather than SAMPLE.  5BIT uses RBAUD, and adds a constant of hex 40
- to each 5-bit character received.
- Note: RXREV does affect the sense of the SAMPLE data.
- RXREV should however not be changed while capturing data.
**Description:**


---

### RXRev   ON/OFF Default: OFF
**Mode:**   Baudot and ASCII RTTY/AMTOR Host: RX
**Parameters:**
- ON — Received data polarity is reversed (mark-space reversal).
- OFF — Received data polarity is normal.
- Use the RXREV Command to invert the polarity of the data demodulated from the
- received mark and space tones.
- In some cases, you may be trying to copy a station that's transmitting "upside
- down" although it is receiving your signals correctly.  This is especially true
- when listening to signals in the Short Wave bands.  Set RXREV ON to reverse the
- data sense of received signals.
- RXREV operates only at RBAUD and ABAUD speeds up to 150 baud.
- PK-232 OPERATING MANUAL                                    
- COMMAND SUMMARY
- The rest of this page is blank
- SAmple “n” Immediate Command
- Mode: Command Host: SA
- “n"     —         20 to 255 specifies the sampling rate in baud.
- This is a new operating mode with the Summer 1991 PK-232 firmware release for
- advanced users interested in decoding unknown synchronous data transmissions.
- SAMPLE is similar to the SBIT and 6BIT modes, but operates on synchronous data,
- whereas 5B1T and 6BIT are used on signals known to be asynchronous.
- SAMPLE syncs up on any regularly-paced data transmission and samples the data
- once per bit, packaging the data in groups and sending the groups to the user for
- further analysis.  The user can use SAMPLE to capture data bits from a
- synchronous transmission, such as FEC, TDM or an 'unknown model' not identified
- by the SIGNAL command.  The transmission is actually sampled several times per
- data bit.  The PK-232 does a majority vote an the last few samples to represent
- the value of the data bit.
- One use for the SAMPLE command is to record the output to a disk file, then write
- a program to analyze the results for synchronous/asynchronous, bit sync patterns,
- data decoding, etc.
- SAMPLE data is captured in 6-bit units; the order of bit reception is MSB first,
- LSB last.  The TNC sends the data unit to the user with a constant of hex 30
- added to each unit, the same as the 6BIT command.  The 6-bit unit is a compromise
- between hexadecimal and 8-bit binary output, The 6-bit unit yields shorter disk
- files than 4-bit hexadecimal characters, but encounters no interference from
- terminal communications programs and the TNC's Converse and Command modes.  The
- 6-bit unit's range of $30-6F falls within the printable ASCII range, allowing the
- TNC to insert end-of-line carriage returns that can be ignored by the user's
- analysis software.
- To use SAMPLE, set ACRDISP to a non-zero value such as 77.  This will break up
- the recorded disk file into lines.  Tune in the signal, set WIDESHFT ON or OFF as
- needed, and get the transmission rate from the SIGNAL command.  Now type "SAMPLE
- (rate)".  As an example, SIGNAL may identify a transmission as 96 baud TDM; in
- this case type the following:
- SAMPLE 96 <Carriage Return>
- Now begin the capture to a disk file with the terminal program. At the end of the
- session, edit the disk file and remove any TNC commands that were echoed before
- or after the received data.
- Occasionally SIGNAL will identify a Baudot transmission at a rate that SAMPLE
- cannot sync up on.  This would happen if the Baudot signal had a stop bit
- duration 1.5 times the data bit duration.  In this case, SAMPLE at twice the baud
- rate and compensate for the doubled data bits in the analysis software. Note that
- it might be more useful to let the TNC do the start/stop bit work by using the
- 5BIT command rather than SAMPLE.  5BIT uses RBAUD, and adds a constant of hex 40
- to each 5-bit character received.
- Note: RXREV does affect the sense of the SAMPLE data.
- RXREV should however not be changed while capturing data.
**Description:**


---

### SAmple “n” Immediate Command
**Mode:** Command Host: SA
**Description:**
“n"     —         20 to 255 specifies the sampling rate in baud. This is a new operating mode with the Summer 1991 PK-232 firmware release for advanced users interested in decoding unknown synchronous data transmissions. SAMPLE is similar to the SBIT and 6BIT modes, but operates on synchronous data, whereas 5B1T and 6BIT are used on signals known to be asynchronous. SAMPLE syncs up on any regularly-paced data transmission and samples the data once per bit, packaging the data in groups and sending the groups to the user for further analysis.  The user can use SAMPLE to capture data bits from a synchronous transmission, such as FEC, TDM or an 'unknown model' not identified by the SIGNAL command.  The transmission is actually sampled several times per data bit.  The PK-232 does a majority vote an the last few samples to represent the value of the data bit. One use for the SAMPLE command is to record the output to a disk file, then write a program to analyze the results for synchronous/asynchronous, bit sync patterns, data decoding, etc. SAMPLE data is captured in 6-bit units; the order of bit reception is MSB first, LSB last.  The TNC sends the data unit to the user with a constant of hex 30 added to each unit, the same as the 6BIT command.  The 6-bit unit is a compromise between hexadecimal and 8-bit binary output, The 6-bit unit yields shorter disk files than 4-bit hexadecimal characters, but encounters no interference from terminal communications programs and the TNC's Converse and Command modes.  The 6-bit unit's range of $30-6F falls within the printable ASCII range, allowing the TNC to insert end-of-line carriage returns that can be ignored by the user's analysis software. To use SAMPLE, set ACRDISP to a non-zero value such as 77.  This will break up the recorded disk file into lines.  Tune in the signal, set WIDESHFT ON or OFF as needed, and get the transmission rate from the SIGNAL command.  Now type "SAMPLE (rate)".  As an example, SIGNAL may identify a transmission as 96 baud TDM; in this case type the following: SAMPLE 96 <Carriage Return> Now begin the capture to a disk file with the terminal program. At the end of the session, edit the disk file and remove any TNC commands that were echoed before or after the received data. Occasionally SIGNAL will identify a Baudot transmission at a rate that SAMPLE cannot sync up on.  This would happen if the Baudot signal had a stop bit duration 1.5 times the data bit duration.  In this case, SAMPLE at twice the baud rate and compensate for the doubled data bits in the analysis software. Note that it might be more useful to let the TNC do the start/stop bit work by using the 5BIT command rather than SAMPLE.  5BIT uses RBAUD, and adds a constant of hex 40 to each 5-bit character received. Note: RXREV does affect the sense of the SAMPLE data. RXREV should however not be changed while capturing data.

_______________________________________________________________________________

---

### SELfec aaaa[aaa]                                       Immediate Command
**Mode:** AMTOR FEC                                        Host: SE
**Parameters:**
- aaaa  -   Specifies the distant station's SELective CALling code (SELCALL).
**Description:**
The SELFEC command starts a SELective FEC (Mode Bs) transmission to a specific  distant station when you enter that station's SELCALL (SELective CALLing) code. The SELFEC command must be accompanied by a unique character sequence (aaaa)  that contains four or seven alphabetic characters.  You do not have to type the  SELCALL a second time if you intend to call the same station again right away. See MYSELCAL and MYIDENT to enter your 4- and 7-character SELCALLs.  Other AMTOR  commands are ACHG, ACRRTTY, ADELAY, ALFRTTY, ARQTMO, EAS, HEREIS and RECEIVE. _______________________________________________________________________________

---

### SEndpac "n"                                       Default: $0D <CTRL-M>
**Mode:** Packet                                      Host: SP
**Parameters:**
- "n"  -    0 to $7F (0 to 127 decimal) specifies an ASCII character code.
**Description:**
Use the SENDPAC command to select the character used to cause a packet to be  sent in Converse Mode.  The parameter "n" is the ASCII code for the character  you want to use to force your input to be sent.  Use default SENDPAC value $0D  for ordinary conversation with ACRPACK ON to send packets at natural intervals. _______________________________________________________________________________

---

### SIgnal                                                 Immediate Command
**Mode:**  All                                             Host: SI
**Description:**
_______________________________________________________________________________ SIGNAL is an immediate command that causes the PK-232 to enter the Signal  Identification and Acquisition Mode (SIAM).  The PK-232 will respond with: Opmode  was  BAudot Opmode  now  SIgnal After a few seconds the PK-232 will show the signals baud rate.  A few seconds  later it will identify the signal type.  _______________________________________________________________________________

---

### SLottime "n"                                      Default: 30 (300 msec.)
**Mode:** Packet                                      Host: SL
**Parameters:**
- "n"  -    0 to 250 specifies the time the PK-232 waits between generating random 
- numbers to see if it can transmit.
**Description:**
The SLOTTIME parameter works with the PPERSIST and PERSIST parameters to achieve  true p-persistent CSMA (Carrier-Sense Multiple Access) in Packet operation.

_______________________________________________________________________________

---

### SQuelch ON|OFF                                              Default: OFF
**Mode:** Packet                                                Host: SQ
**Parameters:**
- ON   -    Your PK-232 responds to positive-going squelch voltage.
- OFF  -    Your PK-232 responds to negative-going squelch voltage.
**Description:**
Normally, your PK-232 uses its CSMA (Carrier Sense Multiple Access) circuit to  decide whether or not it is clear to transmit on a packet channel.  If there are  non-packet signals on the channel you're using (such as voice), you will want to  use true RF-carrier CSMA by monitoring the squelch line voltage from your radio.   If SQUELCH is OFF (default) the PK-232 inhibits transmissions when there is a  POSITIVE voltage on the Radio connectors squelch input line.  When there is no  voltage or NO CONNECTION to this pin, the PK-232 allows packets to be sent. When SQUELCH is ON, the PK-232 will inhibit packet transmissions when there is 0  volts applied to the squelch input pin on the Radio connector. _______________________________________________________________________________

---

### SRXall ON|OFF                                               Default: OFF
**Mode:** AMTOR                                                 Host: SR
**Parameters:**
- ON   -    Receive ALL selective (SELFEC) transmissions.
- OFF  -    Receive only SELCALL-addressed SELFEC transmissions.
**Description:**
SRXALL permits the reception of selectively coded inverse FEC signals normally  not available for decoding.  Set SRXALL ON to activate this feature. _______________________________________________________________________________

---

### STArt "n"                                         Default: $11 <CTRL-Q>
**Mode:** All                                         Host: ST
**Parameters:**
- "n"   -   0 to $7F (0 to 127 decimal) specifies an ASCII character code.
**Description:**
Use the START command to choose the user START character (default <CTRL-Q>) you  want to use to restart output FROM the PK-232 TO the terminal after it has been  halted by typing the user STOP character.  See the XFLOW command. _______________________________________________________________________________

---

### STOp "n"                                          Default: $13 <CTRL-S>
**Mode:** All                                         Host: SO
**Parameters:**
- "n"   -   0 to $7F (0 to 127 decimal) specifies an ASCII character code.
**Description:**
Use the STOP command to select the user STOP character (default <CTRL-S>) you  will use to stop output FROM the PK-232 TO the terminal.  See the XFLOW command.

_______________________________________________________________________________

---

### TBaud "n"                                              Default: 1200 bauds
**Mode:** All                                              Host: TB
**Parameters:**
- "n"   -   Specifies the data rate in bauds, on the RS-232 serial I/O port.
**Description:**
TBAUD sets the baud rate you are using to communicate with the PK-232 from your  terminal or computer.  Use TBAUD to set terminal rates not covered by the  autobaud routine, such as 110 and 600 bauds.  Set TBAUD to specify the terminal  baud rate to be activated at the next power-on or RESTART.  A warning message  reminds you of this.  Be sure you can set your terminal for the same rate. The TBAUD command supports the following serial port data rates of 45, 50, 57,  75, 100, 110, 150, 200, 300, 400, 600, 1200, 2400, 4800 and 9600 bauds. _______________________________________________________________________________

---

### TClear                                                 Immediate Command
**Mode:** Command                                          Host: TC
**Description:**
_______________________________________________________________________________ The TCLEAR command clears your PK-232's transmit buffer and cancels any further  transmission of data when in the Baudot, ASCII, AMTOR or Morse operating modes.   In Packet Mode, all data is cleared except for a few remaining packets. You must be in the Command Mode to use TCLEAR. _______________________________________________________________________________

---

### TDBaud "n"                                                  Default: 96
**Mode:** TDM                                                   Host: TU
**Parameters:**
- "n"   -   Specifies the data rate in bauds of the TDM signal you are receiving.
**Description:**
The default value of n is 96.  TDB can be set to 0-200, but only some of these  are legal values: 1-channel:  48,  72,  96 2-channel:  86,  96, 100 4-channel: 171, 192, 200 No error checking is done for values other than above.  Bad values result in an  internal TDBAUD of 96. _______________________________________________________________________________

---

### TDChan "n"                                                  Default: 0
**Mode:** TDM                                                   Host: TN
**Parameters:**
- "n"   -   Specifies the TDM channel number.
**Description:**
"n" selects which data channel (default 0) to separate out from the multiplexed  TDM signal.  n can be set to 0-3, but only some of these have unique effects:

1-channel: No effect. 2-channel: 0 and 2 show Channel A. 1 and 3 show Channel B. 4-channel: 0 shows Channel A. 1 shows Channel B. 2 shows Channel C. 3 shows Channel D. _______________________________________________________________________________ 

---

### TDm                                                    Immediate Command
**Mode:** TDM                                              Host: TV
**Description:**
_______________________________________________________________________________ TDM is an immediate command that places the PK-232 in the TDM receive mode.   TDM stands for Time Division Multiplexing, also known as Moore code and is the  implementation of CCIR Recommendation 342. Use the PK-232 SIGNAL command first to determine the bit rate and to make sure  that the signal is actually TDM.  The SIGNAL command can detect one or two  channel TDM transmissions. The TDM command forces bit phasing; do this when changing frequency to another  TDM signal.  This is also useful when the PK-232 synchronizes on the wrong bit  in the character stream, which is likely on a signal which is idling. TDM stations idle MOST of the time, so you may have to leave the PK-232  monitoring for an hour or two before any data is received. _______________________________________________________________________________

---

### TIme "n"                                          Default: $14 <CTRL-T>
**Mode:** All                                         Host: TM
**Parameters:**
- "n"   -   0 to $7F (0 to 127 decimal) specifies an ASCII character code.
**Description:**
The TIME command specifies which control character sends the time-of-day in the  text you type into the transmit buffer or into a text file stored on disk. At transmit time, the PK-232 reads the embedded control code (default <CTRL-T>),  reads the time-of-day from the PK-232's internal clock and then sends the time  to the radio in the data transmission code in use at that time.  If the DAYTIME  has not been set, and a control-T will cause the PK-232 to send an asterisk (*). When DAYSTAMP is set ON, the date is transmitted with the time. NOTE:  The TIME command cannot be embedded in CTEXT, BTEXT, MTEXT or AAB.

_______________________________________________________________________________

---

### TMail ON|OFF                                                Default: OFF
**Mode:** AMTOR                                                 Host: TL 
**Parameters:**
- ON    -   The PK-232MBX operates as a personal AMTOR BBS or MailDrop.
- OFF   -   The PK-232MBX only operates as a normal CCIR 476 or 625 controller. 
**Description:**
The PK-232's MailDrop is a personal mailbox that uses a subset of the  W0RLI/WA7MBL PBBS commands and is similar to operation of APLINK stations.  When  TMAIL is ON and another station establishes an ARQ link with your MYSELCAL or  MYIDENT, the remote AMTOR station may leave messages for you or read messages  from you.  Third-party messages are not accepted by your AMTOR MailDrop unless  3RDPARTY is ON. See the MDCHECK, TMPROMPT, MDMON, MTEXT, MMSG MYSELCAL and MYIDENT commands. _______________________________________________________________________________

---

### TMPrompt text                                          Default: (see text)
**Mode:** AMTOR/MailDrop                                   Host: Tp
**Parameters:**
- text  -   Any combination of characters and spaces up to a maximum of 80 bytes.
**Description:**
TMPROMPT is the command line sent to a calling station by your AMTOR MailDrop in  response to a Send message command.  The default text is: "GA subj/GA msg, '/EX' to end." Text before the first slash is sent to the user as the subject prompt; text  after the slash is sent as the message text prompt.  If there is no slash in the  text, the subject prompt is "SUBJECT:" and the text prompt is from TMPROMPT. _______________________________________________________________________________

---

### TRACe ON|OFF                                                Default: OFF
**Mode:** Packet/FAX/Baudot/AMTOR                               Host: TR
**Parameters:**
- ON   -    Trace function is activated.
- OFF  -    Trace function is disabled.
**Description:**
Packet: The TRACE command activates the AX.25 protocol display.  When TRACE is ON all  received frames are displayed in their entirety, including all header  information.  The TRACE display is shown as it appears on an 80-column display.   The following monitored frame is a sample: W2JUP*>TESTER <UI>: This is a test message packet. Byte                Hex                   Shifted ASCII         ASCII 000: A88AA6A8 8AA460AE 6494AAA0 406103F0  TESTER0W2JUP 0.x  ......`.d...@a.. 010: 54686973 20697320 61207465 7374206D  *449.49.0.:29:.6  This is a test m 020: 65737361 67652070 61636B65 742E0D    299032.80152:..   essage packet... The byte column shows the offset into the packet of the first byte of the line. The hex display column shows the next 16 bytes of the packet, exactly as  received, in standard hex format.  The shifted ASCII column decodes the high- order seven bits of each byte as an ASCII character code.  The ASCII column  decodes the low-order seven bits of each byte as an ASCII character code. FAX: When Operating in FAX mode, TRACE is ON, and PRFAX is OFF, the graphics escape  sequences and dot data are sent to the terminal with each byte expanded to two  Hexadecimal characters.  This helps get around the limitations of many terminal  programs that do not allow 8-bit data to be saved to disk as an ASCII file. Interspersed command prompts and even the L and R commands would have no  effect on the final data and it could be translated back to binary data with a  computer program. _______________________________________________________________________________

---

### Trans                                                  Immediate Command
**Mode:** All                                              Host: Not Supported
**Description:**
_______________________________________________________________________________ TRANS is an immediate command that switches the PK-232 switch from the Command  Mode to Transparent Mode.  The current state of the radio link is not affected. Transparent Mode is primarily useful for computer communications.  In  Transparent Mode "human interface" features such as input editing, echoing of  input characters, and type-in flow control are disabled. o    Use Transparent Mode for transferring binary or other non-text files. o    To exit the Transparent mode, type the COMMAND character (default <CTRL-C>)  three times within the time period set by CMDTIME (default 1 Second). _______________________________________________________________________________

---

### TRFlow ON|OFF                                               Default: OFF
**Mode:** All                                                   Host: TW
**Parameters:**
- ON   -    Software flow control for the computer or terminal RECEIVING data is 
- activated in Transparent Mode.
- OFF  -    Software flow control for the computer or terminal RECEIVING data is 
- disabled in Transparent Mode.
**Description:**
When TRFLOW is ON, the type of flow control used by the computer RECEIVING data  in Transparent Mode is determined by how START and STOP are set. When TRFLOW is OFF, only "hardware" flow control (RTS, DTR) is available to the  computer RECEIVING data from the PK-232 in Transparent Mode. If TRFLOW is ON, and START and STOP are set to values other than zero, software  flow control is enabled for the user's computer or terminal.  The PK-232  responds to the user START and user STOP characters while remaining transparent  to all other characters from the terminal. _______________________________________________________________________________

---

### TRIes "n"                                                   Default: 0
**Mode:** Packet                                                Host: TI
**Parameters:**
- "n"  -    0 to 15 specifies the current RETRY level on the selected input 
- channel.
**Description:**
TRIES retrieves (or forces) the count of "retry counter" on the data channel  presently selected. If you type TRIES without an argument, the PK-232 returns the current number of  tries if an outstanding unacknowledged frame exists.  If no outstanding  unacknowledged frame exists, the PK-232 returns the number of tries required to  get an ACK for the previous frame. If you type TRIES with an argument the "tries" counter is forced to the entered  value.  Using this command to force a new count of tries is not recommended. _______________________________________________________________________________

---

### TXdelay "n"                                       Default: 30 (300 msec.)
**Mode:** Packet, Baudot and ASCII                    Host: TD
**Parameters:**
- "n"  -    0 to 120 specifies ten-millisecond intervals.
**Description:**
The TXDELAY command tells your PK-232 how long to wait before sending packet  frame data after keying your transmitter's PTT line (default 300 msec). All transmitters need some amount of start-up time to put a signal on the air.   The default value of 300 msec should work with almost all transceivers. In fact many of the newer transceivers can use smaller TXDELAY values.  Crystal  controlled transceivers can often use smaller values as well.  On the other  hand, tube-type transceivers and amplifiers can require a longer time to switch  and may require TXDELAY to be increased.  Experiment with the value to determine  the shortest setting you can use in reliably in Packet. Baudot and ASCII use TXDELAY between PTT ON and the start of transmitted data.

_______________________________________________________________________________

---

### TXFlow ON|OFF                                               Default: OFF
**Mode:** Packet                                                Host: TF
**Parameters:**
- ON   -    Software flow control for the PK-232 is activated in Transparent Mode.
- OFF  -    Software flow control for the PK-232 is disabled in Transparent Mode.
**Description:**
When TXFLOW is ON, the setting of XFLOW determines the type of flow control used  in Transparent Mode by the PK-232 to control TRANSMITTED data. When TXFLOW is OFF, the PK-232 uses only hardware flow control to control  TRANSMITTED data; all data sent to the terminal remains fully transparent. When TXFLOW and XFLOW are ON, the PK-232 uses the Start and Stop characters (set  by XON and XOFF) to control the input from the computer. _______________________________________________________________________________

---

### TXRev ON|OFF                                                Default: OFF
**Mode:** Baudot/ASCII/AMTOR                                    Host: TX
**Parameters:**
- ON   -    Transmit data polarity is reversed (mark-space reversal).
- OFF  -    Transmit data polarity is normal.
**Description:**
Use the TXREV Command to reverse the mark and space in the transmitted AFSK and  FSK signals. In some cases, the station you're working may be receiving inverted data  although it is transmitting in correct polarity.  Set TXREV ON to reverse the  sense of your transmitted signals. TXREV only works on ABAUD and RBAUD at speeds up to 150 baud. _______________________________________________________________________________

---

### UBit "n" ON/OFF                                             Default: 0
**Mode:** All                                                   Host: UB
**Parameters:**
- "n"  -    0 to 255 specifying a User BIT that may be set ON or OFF.
**Description:**
The UBIT is an extension of the CUSTOM command which allows up to 255 ON/OFF  functions to be added to the PK-232 without burdening users with a large number  of commands.  The functions controlled by UBIT are not things that most users  will ever have to change.  Still they are important enough to some users or  application programs that we have included them under the umbrella command UBIT.   The following are examples of how to use the UBIT command: UBIT 5         Returns the present status of UBIT 5 UBIT 1 ON      Sets the function controlled by UBIT 1 to ON UBIT 10 T      Toggles the state of the function controlled by UBIT 10 UBIT           Returns the state of the last UBIT value that was accessed

Listed below are the UBIT functions and the default state that presently have been assigned.  The default state of each UBIT is always shown first.

UBIT 0: 	ON:  The PX-232 will discard a received packet if the signal is too weak to light the DCD LED. OFF: The PK-232 will receive a packet regardless of the DCD status or the THRESHOLD control setting.

UBIT 1: 	OFF: Entering the command MONITOR ON or MONITOR YES causes the MONITOR command to be set to 4. ON:  Entering the command MONITOR ON or MONITOR YES causes the MONITOR command to be set to 6.

UBIT 2:  	ON:  A Break signal received on the RS-232 line forces the PK-232 into Command mode from all modes except HOST mode. OFF: A Break signal on the RS-232-line is ignored by the PK-232.

UBIT 3:  	OFF: Multiple connect Packet channels are numbd-red from 0-9. ON:  Multiple connect Packet channels are numbered A-J.

UBIT 4:  	ON:  When transmitting in Baudot, the PK-232 inserts the FIGS after a space just prior to sending any figures space><FIGS><number>). This permits receiving stations to decode groups of figures correctly regardless of the USOS setting. OFF: The PK-232 will not insert the FIGS character after each space. MARS operators may want to set UBIT 4 OFF for literal operation.

UBIT 5:   OFF: The PK-232 will always power up in Command Mode. ON:  The PK-232 will remain in the last mode Converse, Command or Transparent) provided the battery jumper enabled.

UBIT 6: 	OFF: In Packet, monitoring is disabled when in the Transparent mode. ON:  Packet monitoring is active in the Transparent mode.  MFROM, MT0, MRPT, MONITOR, MCON, MPROTO, MSTAMP, MXMIT, CONSTAMP and MBX are all active. UB1T 7:   OFF: In Morse receive, the character ..-- prints as a “^” ON:  In Morse, the character ..-- prints as a <Carriage Return>.

UBIT 8: 	OFF: In the Morse mode, the PK-232's modem is configured for standard Al-A single-tone ON/OFF keyed Morse. ON:  In the Morse mode, the PK-232's modem is configured for 2-tone FSK Morse operation on both transmit and receive.  WIDESHFT, RXREV and TXREV are all active.

UBIT 9:   ON:  In AMTOR a received WRU character (FIGS-D) or in Pactor a  <CTRL-E> will cause the Auto-Answerback text to be sent regardless of the setting of WRU.  In AMTOR this is subject to the setting of the CODE command. OFF: In AMTOR and Pactor a received WRU character will have no effect.

UBIT 10: 	OFF: polling in the HOST mode is subject to HPOLL and must be done for all changes in status. ON:  Status changes (e.g., Idle to Tfc) in AMTOR, FAX, TDM or NAVTEX causes the PK-232 to issue the following host block: SOH $50 n ETB where n is $30-36, the same number that the OPMODE command furnishes. This block is subject to EPOLL.

UBIT 11: 	ON:  A "Connected" message appears when an AMTOR or Pactor ARQ link is first established using seven-character SELCALLs (CCIR 625). OFF: No Connected message appears at the start of ARQ communications.

UBIT 12: 	OFF: The Packet Morse ID (MID) is ON/OFF keying of the low tone. ON:  The Packet Morse ID is sent in 2-tone FSK with the low tone being key-down and the high tone representing key-up.  Use this setting to keep other stations from sending a packet during the Morse ID.

UBIT 13: 	OFF: MailDrop Connect status messages are always sent to the local user, regardless of the setting of MDMON. ON:  Remote user dialog and Connect status messages with the MailDrop are shown only if MDMON is ON.

UBIT 14: 	OFF: In Packet, the transmit buffer for data sent from the computer to the PK-232 is limited only by available PK-232 memory. ON:  In Packet, the serial flow control will permit only a maximum of 7 I-Frames to be held by the PK-232 before transmission.  This solves a problem with the YAPP binary file transfer program which relies on a small TNC transmit buffer to operate correctly.

UBIT 15:  Not used in the PK-232.

UBIT 16:  Not used in the PK-232.

UBIT 17: 	OFF: Morse, Baudot, ASCII and AMTOR transmissions start when commanded by the user or an application program. ON:  Morse, Baudot, ASCII and AMTOR transmissions will not begin until the channel is clear of signals.  The channel is considered clear when both the DCD and the Squelch input are inactive.  The PERSIST and SLOTTIME delay functions are used if PPERSIST is ON, otherwise he DWAIT time is used.

UBIT 18: OFF:  In Packet operation, the FRACK (or FRICK if enabled) timer is used to retry packets that were not acknowledged. ON:  An experimental Master/Slave relationship is established when a Packet connection is made.  This is designed for meteor scatter operation and is described in detail under the FRICK command.

UBIT 19: Not used in the PK-232.

UBIT 20: Not used in the PK-232.

UBIT 21: Not used in the PK-232.

UBIT 22:  ON:  In the Packet mode, the PK-232 will respond to the receipt of an UNPROTO frame addressed to QRA by sending an LTNPROTO ID packet frame within 1 to 10 seconds.  This feature is compatible with TAPR's ANSWRQTLA command. OFF: The PK-232 does not respond to UNPROTO frames addressed to QRA.

UBIT settings 23 and above are reserved for future expansion. _______________________________________________________________________________

---

### Ucmd   “n” [x] Default: 0
**Mode:** All Host: UC
**Parameters:**
- “n” - 0 to 4 specifying a User command that may be set.
**Description:**
The UCMD command is similar to the UBIT command.  UCMD allows seldom used commands that take numeric arguments (rather than ON/OFF) to be set.  Presently the functions controlled by UCMD are Pactor settings that most users will never have to change.  Still they are important enough to some users or application programs that we have included them under the umbrella command UCMD. The following are examples of how to use the UCMD command: UCMD 2 Returns the present value of UCMD 2. UCMD 1 3 Sets the function controlled by UCMD I to 3. UCMD 3 Y Returns the value of UCMD 3 to its default. UCMD Returns the value of the last UCMD value that was accessed. Listed below are the UCMD functions and the default value that presently has been assigned. UCMD 0: [x] x = 0-30, default 3. Sets the number of correct Pactor packets in a row that must be received before generating an automatic request to change from 100 to 200 baud. UCMD 1: [x] x = 0-30, default 6. Sets the number of incorrect Pactor packets in a row that must be received before generating an automatic request to change from 200 to 100 baud. UCMD 2: [x] x 0-9, default 2. Sets the number of Pactor packets sent in a speed-up attempt. UCMD 3: [x] x = 0-60, default S. Sets the maximum number of Memory ARQ Pactor packets that are combined to form one good packet.  When this number is exceeded, all stored packets are cleared and Memory ARQ is re-initialized. “UCMD 3 0” disables Memory ARQ.

_______________________________________________________________________________

---

### Unproto call1 [VIA call2[,call3 . . . . , call9]] Default: CQ
**Mode:** Packet Host: UN
**Parameters:**
- Call1 — Call sign to be placed in the TO address field. 
- call2-9 — Optional digipeater call list, up to eight calls.
**Description:**
UNPROTO sets the digipeat and destination address fields of packets sent in the unconnected (unprotocol) mode. Unconnected packets are sent as Unnumbered I-frames (UI frames) with the destination and digipeat fields taken from "call1' through "call9' options.  When a destination is not specified, unconnected packets are sent to “CQ” . Unconnected packets sent from other packet stations can be monitored by setting MONITOR to a value greater than “1” and setting MFROM to ALL. The UNPROTO path and address is also used for beacon packets. _______________________________________________________________________________

---

### USers "n" Default; 1
**Mode:** Packet Host: UR
**Parameters:**
- “n”     — 0 to 10 specifies the number of active simultaneous connections that
- can be established with your PK-232.
**Description:**
USERS affects the way that incoming connect requests are handled.  It does not affect the number of connections you initiate with your PK-232.  For example: USERS 0 allows incoming connections on any free logical channel USERS 1 rejects incoming connections if there are connections on 1 or more logical channels. USERS 2 rejects incoming connections if there are connections on 2 or more logical channels.  And so on, through USERS 10. _______________________________________________________________________________

---

### USOs ON/OFF Default; OFF
**Mode:** Baudot RTTY Host: US
**Parameters:**
- ON     — Letters (LTRS) case IS forced after receiving a space character.  
- OFF   — Letters (LTRS) is NOT forced after receiving a space character.
**Description:**
Use the USOS Command (UnShift On Space) when you want your PK-232 to automatically change from figures to letters after receiving a space character. When using Baudot RTTY in poor HF receiving conditions, a received character can be incorrectly interpreted as a FIGURES-SHIFT character, forcing the received data into the wrong case.  Many otherwise good characters received after this will be interpreted as figures (numbers and punctuation), not as the letters sent by the distant station.  USOS ON helps reduce these receiving errors. _______________________________________________________________________________

---

### Vhf  ON/OFF Default: ON
**Mode:** Packet Host: VH
**Parameters:**
- ON — Packet tones are shifted 1000 Hz. 
- OFF — Packet tones are shifted 200 Hz.
**Description:**
Use the VHF Command for immediate software control of the PK-232's modem tones. Changing components or switch settings is not required. Set VHF ON for VHF operation (default), and set VHF OFF for HF packet operation. NOTE: Be sure to change HBAUD to 300 bauds when operating below 28 MHz. _______________________________________________________________________________

---

### WHYnot  ON/OFF Default: OFF
**Mode:** Packet Host: WN
**Parameters:**
- ON — The PK-232 generates a reason why received packets were not
- displayed.
- OFF — This function is disabled.
**Description:**
During packet operation, the PK-232 may receive many packets that are not displayed.  Turning WHYNOT on will cause the PK-232 to display a message explaining the reason the received packet was not displayed to the screen.  The messages and their meanings are shown below: PASSALL: The received packet frame had errors, and PASSALL was off, preventing the packet from being displayed to the screen. DCD Threshold: The Threshold control was set too far counter clockwise.  The DCO LED was off when the packet was received. MONITOR: The MONITOR value was set too low to receive this frame. MCON: MCON was set too low to receive this type of frame. MPROTO: MPROTO was set to off, and the received packet was probably a NET/ROM or TCP/IP frame. MFROM/MTO: The frame was blocked by the MFROM or MTO command. MBX: The call sign of the sending station does not match the call sign setting in the MBX command. MBX Sequence: The frame was received out of sequence, probably a retry. Frame too long: Incoming packet frame longer than 330 bytes.  Probably a non- AX.25 frame. Frame too short: Incoming packet frame shorter than 15 bytes. Only seen if PASSALL ON.  Probably noise. RX overrun: Another HDLC byte was received before we could read the previous one out of the HDLC chip. _______________________________________________________________________________

---

### Wideshft  ON/OFF Default: OFF
**Mode:**  Baudot/ASCII RTTY/AMTOR/Pactor Host:  WI
**Parameters:**
- ON   — RTTY tones are shifted 1000 Hz.
- OFF  —    RTTY tones are shifted 200 Hz (emulates 170-Hz shift).
**Description:**
The WIDESHFT command permits the use of the PK-232 on VHF or HF with either wide (1000 Hz) or narrow (200 Hz) shifts.  Many amateur radio VHF and HF RTTY operators use 170 Hz shift.  The PK-232's 200 Hz shift is well within the filter tolerances of any RTTY demodulator in general service.  MARS stations will find WIDESHFT generally compatible with MARS and commercial 850 Hz shift RTTY operations. _______________________________________________________________________________

---

### WOrdout ON/OFF Default: OFF
**Mode:** Baudot, ASCII, AMTOR, Pactor and Morse Host: WO
**Parameters:**
- ON — Typed characters are held in the transmit buffer until a space, CR,
- LF, TAB, RECEIVE, CWID, ENQ or +? characters(s) is typed.
- OFF — Typed characters are sent directly to the transmitter.
**Description:**
Use the WORDOUT Command to choose whether or not you can edit while entering text for transmission. When WORDOUT is on, each character you type is held in a buffer until space, a carriage return, a line fed, ENQ character ($05, <CTRL-E->) or the +?. You can edit words before the transmit buffer's contents are sent to the radio.  When WORDOUT is OFF, each character you type is sent to the radio just as you typed it, without any delay. _______________________________________________________________________________

---

### WRu ON/OFF Default: OFF
**Mode:**  Baudot, ASCII Host: WR
**Parameters:**
- ON — Your Auto-AnswerBack is sent after a distant station's WRU?
- OFF — Your Auto-AnswerBack is NOT sent after a distant station's WRU?
**Description:**
Use the WRU command in Baudot and ASCII to enable or disable your PK-232's Automatic-AnswerBack feature. When WRU is ON, your PK-232 sends the AnswerBack on receipt of a distant station's WRU? reqruest (“FIGS-D” or '$' in Baudot or a <CTRL-E> in ASCII).  Your PK-232 keys your transmitter, sends the text stored in the AnswerBack field (AAB), then unkeys your transmitter and returns to receive. The WRU function is defaulted ON in AMTOR as per the CCIR recommendation. To allow for special applications where it may be helpful to disable the WRU function in AMTOR.  UBIT 9 or bit 9 of the CUSTOM command controls this feature. _______________________________________________________________________________

---

### XBaud "n" Default: 0
**Mode:** ASCII/Baudot Host: XB
**Parameters:**
- ”n” —        Specifies an exact baud rate used in receiving ASCII and Baudot
- RTTY.
**Description:**
XBAUD enables hardware decoding of ASCII and Baudot signals using the PK-232's 8530 Serial Communications Controller IC.  This can allow the PK-232 to achieve better copy of these signals as well as allow non-standard data rates to be received.  One disadvantage of XBAUD is that the software commands RXREV and TXREV can not be used to invert the data so the correct sideband must be used. To use XBAUD simply enter the data rate that is desired in either ASCII or Baudot modes.  For example a 20-meter Baudot RTTY enthusiast may want to set XBAUD to 45 to improve copy of weak signals on the band. It is important to remember that XBAUD overrides both the RBAUD (in Baudot) and ABAUD (in ASCII) as well as the inverting commands TXREV and RXREV.  This means that if XBAUD has been set to 45 for Baudot operation, it should be reset to 0 before changing modes to ASCII. Otherwise the PK-232 will attempt to receive ASCII at 45 bauds! To help reduce the chance of this error occurring, the PK232 will disable XBAUD by setting it to 0 every time the SIGNAL Identification mode is used and the command OK is entered. The XBAUD command supports data rates from 1 to 9600 bits per second although the PK-232 internal modem only supports data rates to 1200 baud. _______________________________________________________________________________

---

### IFlow  ON/OFF Default: ON 
**Mode:**  All Host: XW
**Parameters:**
- ON — XCN/XOFF (software) flow control is activated.
- OFF — XON/XOFF flow control is disabled - hardware flow control is enabled.
**Description:**
When XFLOW is ON, software flow control is in effect - it's assumed that the computer or terminal will respond to the PK-232's Start and Stop characters defined by the XON and XOFF commands.  Similarly, the PK-232 will respond to the computer’s start and stop characters defined by START and STOP. When XFLOW is OFF, the PK-232 sends hardware flow control commands via the CTS and is controlled via either the RTS or the DTR line. _______________________________________________________________________________

---

### Xmit Immediate Command
**Mode:** Baudot/ASCII/Morse and FAX Host: XM
**Description:**
_______________________________________________________________________________ XMIT is an immediate command that keys your radio's PTT line and prepares the radio to receive outbound data and Morse characters from the PK-232.  The XMIT Command can only be used from the Command Mode. XMIT switches your PK-232 to either Converse Mode or Transparent Mode, depending on the setting of CONMODE.  Typing the CWID or the RECEIVE character will return you to receive.  Typing RCVE from the Command mode will also return to receive. _______________________________________________________________________________

---

### XMITok   ON/OFF Default:  ON
**Mode:**  All Host: XO
**Parameters:**
- ON — Transmit functions (PTT line) are active.
- OFF   — Transmit  functions (PTT line) are disabled.
**Description:**
When XMITOK is OFF, the PTT line to your transmitter is disabled - the transmit function is inhibited.  All other PK-232 functions remain the same. Your PK-232 generates and sends packets as requested, but does not key the radio's PTT line. Use the XMITOK command at any time to ensure that your PK-232 does not transmit. Turning XMITOK OFF can help enable full break-in CW (QSK) on certain transceivers. _______________________________________________________________________________

---

### XOFF “n” Default:$131 <CTRL-S>
**Mode:** All Host: XF
**Parameters:**
- “n” —  0 to $7F  (0 to 127 decimal) specifies an ASCII character code.
**Description:**
Use XOFF to select the Stop character to be used to stop input from the computer or terminal. The Stop character default value is <CTRL-S> for computer data transfers. _______________________________________________________________________________

---

### XON “n” Default: $11 <CTRL-Q>
**Mode:** All Host: XN
**Parameters:**
- “n” —  0 to $7F  (0 to 127 decimal) specifies an ASCII character code.
**Description:**
XON selects the PK-232 Start character that is sent to the computer or terminal to restart input from that device. The start character default value is <CTRL-Q> for computer data transfers.
