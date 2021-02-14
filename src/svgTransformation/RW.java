package svgTransformation;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import gnu.io.*;

public class RW implements SerialPortEventListener {

    private static CommPortIdentifier portId;
    private static Enumeration portList;
    private SerialPort serialPort;
    private boolean portStatus = false;
    private static Charset utf8 = StandardCharsets.UTF_8;
    private String defaultPort;

    private int baudrate = 115200; // default value for table
    private int dataBits = SerialPort.DATABITS_8;
    private int stopBits = SerialPort.STOPBITS_1;
    private int parity = SerialPort.PARITY_NONE;

    private InputStream inputStream;
    private OutputStream outputStream;
    private BufferedReader inStream;
    private Thread threadThread;

    // commands for communicating with the table
    private static String status = "#1$\r";
    private static String up = "#1";
    private static String down = "#1";
    private static String left = "#1";
    private static String right = "#1";
    private static String stop = "#1";
    private static String firmware = "#1:v\r";
    private static String startMotor = "#1:A\r";
    private static String stopMotor = "#1:s\r";
    private static String quickMotorStop = "#1:H\r";
    private static String slowMotorStop = "#1:B\r";
    private static String direction = "#1:d";
    private static String motorType = "#1:CL_motor_type=0";
    private static String getDirection = "#1Zd\r";
    private static String setPosition = "#1:D";
    private static String baudrate_STRING = "#1:baud=";
    private static String coordinates = "#1:ZY\r";
    private static String stepMode = "#1:g+";
    private static String revolutionSpeed = "#1:v\r";
    private static String javaErrors = "#1:(JE\r";

    private int[] baudrates = new int[]{110, 300, 600, 1200, 2400, 4800, 9600, 14400, 19200, 38400, 57600, 115200};
    private int[] steps = new int[]{1, 2, 4, 5, 8, 10, 16, 32, 64, 254, 255};

    public RW() {
        openPort();
        if (portStatus) {
            try {
                //int readBufferLength = inputStream.available();
                inStream = new BufferedReader(new InputStreamReader(inputStream), 100);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // do communication with table
            getStatus();
        } else {
            System.out.println("Couldn't open COM3 port");
        }
    }

    public boolean openPort() {

        // determine the name of the serial port on several operating systems
        String osname = System.getProperty("os.name", "").toLowerCase();
        if (osname.startsWith("windows")) {
            // windows
            defaultPort = "COM3";
        } else if (osname.startsWith("linux")) {
            // linux
            defaultPort = "/dev/ttyS0";
        } else if (osname.startsWith("mac")) {
            // mac
            defaultPort = "/dev/tty";
        } else {
            System.out.println("Sorry, your operating system is not supported");
        }

        // check port status
        boolean foundPort = false;
        if (portStatus) {
            System.out.println("Port already open.");
            return false;
        }

        // open serial port
        System.out.println("### RW [OPEN] port");
        portList = CommPortIdentifier.getPortIdentifiers();
        List<String> listOfPorts = new ArrayList<>();
        while (portList.hasMoreElements()) {
            portId = (CommPortIdentifier) portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                listOfPorts.add(portId.getName());
                if (portId.getName().equals(defaultPort)) {
                    foundPort = true;
                    System.out.println("### RW [FOUND] COM3 port!");
                    break;
                }
            }
        }
        //System.out.println(listOfPorts);

        if (!foundPort) {
            System.out.println("No Port found " + portId);
            return false;
        }

        try {
            serialPort = (SerialPort) portId.open("SVGTesting", 500);
            System.out.println("### RW [OPEN] Port COM3");
        } catch (PortInUseException e) {
            System.out.println("Port is already in use!");
        }
        try {
            inputStream = serialPort.getInputStream();
            System.out.println("### RW [GET] input stream");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("No access to input stream");
        }
        try {
            outputStream = serialPort.getOutputStream();
            System.out.println("### RW [GET] output Stream");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("No access to ouput stream");
        }
        try {
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);
            System.out.println("### RW [ADD] event listener");
        } catch (TooManyListenersException e) {
            System.out.println("TooManyListenersException for serial port");
        }
        try {
            serialPort.setSerialPortParams(baudrate, dataBits, stopBits, parity);
            System.out.println("### RW [SET] serial port parameter");
        } catch (UnsupportedCommOperationException e) {
            System.out.println("Interface parameter couldn't be set");
        }

        portStatus = true;
        return true;
    }

    public void closePort() {

        if (portStatus) {
            System.out.println("### RW [TRY] Closing serial port.");
            try {
                serialPort.removeEventListener();
                serialPort.close();
                inputStream.close();
                outputStream.close();
                // update status
                portStatus = false;
            } catch (Exception e) {
                System.out.println("Couldn't close port");
                e.printStackTrace();
            }
        } else {
            System.out.println("Port is already open.");
        }

    }

    public void serialEvent(SerialPortEvent event) {

        String rawInput = null;

        switch (event.getEventType()) {
            case SerialPortEvent.BI:
            case SerialPortEvent.OE:
            case SerialPortEvent.FE:
            case SerialPortEvent.PE:
            case SerialPortEvent.CD:
            case SerialPortEvent.CTS:
            case SerialPortEvent.DSR:
            case SerialPortEvent.RI:
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                System.out.println("Output buffer empty");
                break;
            case SerialPortEvent.DATA_AVAILABLE:
                System.out.println("### RW [NEW] data from table available");
                try {
                    //int readBufferLength = inputStream.available();
                    //byte[] readBuffer = new byte[readBufferLength];
                    rawInput = inStream.readLine();
                    while (rawInput != null && !rawInput.contains("\r")) {
                        System.out.println(rawInput);
                    }

                    inStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
                //readInputStream();
                break;
            default:
                break;
        }
    }

    private void readInputStream() {
        try {
            int readBufferLength = inputStream.available();
            byte[] readBuffer = new byte[readBufferLength];
            System.out.println("### RW [OUTPUT] read buffer length: " + readBuffer.length);
            if (readBufferLength > 0) {
                // read from the serial port
                readBuffer = inputStream.readAllBytes();
                // ->>>>>>> AN Dieser Stelle wirft der Code immer einen Fehler <<<<<<-//

                // print it to the console
                System.out.println(new String(readBuffer, 0, readBufferLength));
            }


            //System.out.println("String from table: " + sb);
            // search for the different answers of the table
//                    if (str.contains("v"))
//                        System.out.println("Firmware version: " + str);
//                    if (str.contains("1"))
//                        System.out.println("Motor is ready");
//                    if (str.contains("ERROR"))
//                        System.out.println("ERROR is there");
//                    if (str.contains("CMGS"))
//                        System.out.println("message Sent");
//                    if (str.contains("CMT")) {
//                        System.out.println("Message Recieved --->");
//                        System.out.println(str);
//                    }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Serial port event listener Exception");
        }
    }

    public void getStatus() {
        writeData(status);
    }


    // all the methods for sending data to the table
    public void setTargetPosition(int value) {

    }

    public void setMaxSpeed(int value) {

    }

    public void setMode(int value) {

    }

    public void setMotorType() {
        writeData(motorType);
    }

    /**
     * writing the command to the table and get the input
     */
    public void getFirmwareVersion() {
        writeData(firmware);
    }

    public void getCurrentPosition() {
        writeData(coordinates);
    }

    public void getRevolutionSpeed() {
        writeData(revolutionSpeed);
    }

    public void getErrors() {
        writeData(javaErrors);
    }

    /**
     * set the direction on the x and y axis
     */
    public void setDirection(int value) {
        // build string for output
        direction = direction + value + "\r";
        writeData(direction);
    }


    /**
     * set the position with integer values from -100000000 till +100000000
     *
     * @param value
     */
    public void setPosition(int value) {
        // build string for output
        setPosition = setPosition + value + "\r";
        writeData(setPosition);

    }


    public void getDirection() {
        writeData(getDirection);
    }

    /**
     * allowed values:
     * 1,2,3,4,8,10,16,32,64,254,255
     */
    public void setStepMode(int value) {

        // check if input is in array
        boolean contains = IntStream.of(steps).anyMatch(x -> x == value);

        if (contains) {
            // build string for the output
            stepMode = stepMode + value + "\r";
            writeData(stepMode);
        } else {
            System.out.println("New motor step couldn't be set, because " + value + " is not an allowed step.");
        }
    }

    /**
     * possible baudrates in xy table from movtec:
     * 1 :   110
     * 2 :   300
     * 3 :   600
     * 4 :  1200
     * 5 :  2400
     * 6 :  4800
     * 7 :  9600
     * 8 : 14400
     * 9 : 19200
     * 10 : 38400
     * 11 : 57600
     * 12 : 115200 (default)
     *
     * @param baudrateValue is setting which rate should be set
     */
    public void setBaudrate(int baudrateValue) {

        // build string for the output
        baudrate_STRING = baudrate_STRING + baudrateValue;

        // get the value for setting the new baudrate
        int newBaudrate = baudrates[baudrateValue + 1];

        try {
            Thread.sleep(50);
            baudrate = newBaudrate;
            outputStream.write(baudrate_STRING.getBytes(utf8));
        } catch (IOException | InterruptedException e) {
            System.out.println(e);
        }
    }

    /**
     * move the table according to the entered command
     *
     * @param command
     */
    public void move(String command) {
        switch (command) {
            case "up":
                writeData(up);
            case "down":
                writeData(down);
            case "left":
                writeData(left);
            case "right":
                writeData(right);
            case "stop":
                writeData(stop);
        }
    }


    public static void moveTo(float x, float y) {

        // convertion to int should be in the controller class
        // build the command for telling the table where to move to

    }

    private void writeData(String command) {
        try {
            byte[] data = command.getBytes(utf8);
            System.out.println("### RW [SEND] bytes: " + Arrays.toString(data));
            //Thread.sleep(200);
            outputStream.write(data);
            System.out.println("### RW [SEND] bytes");
        } catch (IOException e) // | InterruptedException
        {
            System.out.println("### RW [EXCEPTION] can't write to table");
            e.printStackTrace();
        }
    }

    //TODO writing function for zeroing the position of the table

}
