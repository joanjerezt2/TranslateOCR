package org.apertium.recursive;

/**
 * @author Joan Jerez, ToBeIT
 * @serial GPL 3.0
 */

public interface ByteCode {
    byte DROP = 'd';
    byte DUP = '*';
    byte OVER = 'o';
    byte SWAP = 'w';

    byte STRING = 's';
    byte INT = 'i';
    byte PUSHFALSE= 'f';
    byte PUSHTRUE= 't';
    byte PUSHNULL= '0';

    byte JUMP= 'j';
    byte JUMPONTRUE= 'J';
    byte JUMPONFALSE= '?';

    byte AND= '&';
    byte OR= '|';
    byte NOT= '!';


    byte EQUAL= '=';
    byte ISPREFIX= '(';
    byte ISSUFFIX= ')';
    byte ISSUBSTRING= 'c';

    byte EQUALCL= 'q';
    byte ISPREFIXCL= 'p';
    byte ISSUFFIXCL= 'u';
    byte ISSUBSTRINGCL= 'r';

    byte HASPREFIX= '[';
    byte HASSUFFIX= ']';
    byte IN= 'n';

    byte HASPREFIXCL= '{';
    byte HASSUFFIXCL= '}';
    byte INCL= 'N';

    byte GETCASE= 'a';
    byte SETCASE= 'A';

    // Variables

    byte FETCHVAR   = 'v';
    byte SETVAR     = '$';
    byte FETCHCHUNK = '5';
    byte SETCHUNK   = '6';

    // Clips

    byte SOURCECLIP    = 'S';
    byte TARGETCLIP    = 'T';
    byte REFERENCECLIP = 'R';
    byte SETCLIP       = '>';

    // Chunks

    byte CHUNK             = 'C';
    byte APPENDCHILD       = '1';
    byte APPENDSURFACE     = '2';
    byte APPENDALLCHILDREN = '3';
    byte APPENDALLINPUT    = '4';
    byte PUSHINPUT         = '7';
    byte APPENDSURFACESL   = '8';
    byte APPENDSURFACEREF  = '9';

    // Output

    byte OUTPUT    = '<';
    byte BLANK     = 'b';
    byte OUTPUTALL = '@';
    byte CONJOIN   = '+';

    // Other

    byte CONCAT     = '-';
    byte REJECTRULE = 'X';
    byte DISTAG     = 'D';
    byte GETRULE    = '^';
    byte SETRULE    = '%';
    byte LUCOUNT    = '#';
}
