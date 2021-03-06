import edu.indiana.soic.spidal.common.Range;
import org.apache.commons.cli.Option;
import pviz.Plotviz;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class Utils {
    public static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

    public static Record parseFile(BufferedReader reader) throws FileNotFoundException {
        return parseFile(reader, null, false);
    }

    public static Record parseFile(BufferedReader reader, CleanMetric metric, boolean convert) throws FileNotFoundException {
        String myLine = null;
        try {
            while ((myLine = reader.readLine()) != null) {
                String[] array = myLine.trim().split(",");
                if (array.length >= 3) {
                    int permNo = Integer.parseInt(array[0]);
                    Date date = Utils.formatter.parse(array[1]);
                    if (date == null) {
                        System.out.println("Date null...............................");
                    }
                    String stringSymbol = array[2];
                    if (array.length >= 7) {
                        double price = -1;
                        if (!array[5].equals("")) {
                            price = Double.parseDouble(array[5]);
                            if (convert) {
                                if (price < 0) {
                                    price *= -1;
                                    if (metric != null) {
                                        metric.negativeCount++;
                                    }
                                }
                            }
                        }

                        double factorToAdjPrice = 0;
                        if (!"".equals(array[4].trim())) {
                            factorToAdjPrice = Double.parseDouble(array[4]);
                        }

                        double factorToAdjVolume = 0;
                        if (!"".equals(array[3].trim())) {
                            factorToAdjVolume = Double.parseDouble(array[3]);
                        }

                        int volume = 0;
                        if (!array[6].equals("")) {
                            volume = Integer.parseInt(array[6]);
                        }

                        return new Record(price, permNo, date, array[1], stringSymbol, volume, factorToAdjPrice, factorToAdjVolume);
                    } else {
                        return new Record(-1, permNo, date, array[1], stringSymbol, 0, 0, 0);
                    }
                }
            }
        } catch (IOException | ParseException e) {
            throw new RuntimeException("Failed to read content from file", e);
        }
        return null;
    }

    public static void createDirectory(String directoryName) {
        File theDir = new File(directoryName);
        if (!theDir.exists()) {
            System.out.println("creating directory: " + directoryName);
            try {
                theDir.mkdirs();
            } catch (SecurityException se) {
                //handle it
            }
        }
    }

    private static void copyFileUsingFileChannels(File source, File dest)
            throws IOException {
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            if (inputChannel != null) {
                inputChannel.close();
            }
            if (outputChannel != null) {
                outputChannel.close();
            }
        }
    }

    public static Date parseDateString(String date) {
        try {
            return formatter.parse(date);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse date", e);
        }
    }

    public static Date addYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.YEAR, 1);
        return cal.getTime();
    }

    public static Date addMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MONTH, 1);
        return cal.getTime();
    }

    public static Date addDays(Date data, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(data);
        cal.add(Calendar.DATE, days);
        return cal.getTime();
    }

    public static String getMonthString(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR) + "_" + (cal.get(Calendar.MONTH) + 1);
    }

    public static String getDateString(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        String month = String.format("%02d", (cal.get(Calendar.MONTH) + 1));
        String day = String.format("%02d", (cal.get(Calendar.DATE)));
        return cal.get(Calendar.YEAR) + month + day;
    }

    public static String getYearString(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR) + "";
    }

    public static String dateToString(Date date) {
        return formatter.format(date);
    }

    public static VectorPoint parseVectorLine(String line) {
        String parts[] = line.trim().split(" ");
        if (parts.length > 0 && !(parts.length == 1 && parts[0].equals(""))) {
            int key = Integer.parseInt(parts[0]);
            double cap = Double.parseDouble(parts[1]);

            int vectorLength = parts.length - 2;
            double[] numbers = new double[vectorLength];
            for (int i = 2; i < parts.length; i++) {
                numbers[i - 2] = Double.parseDouble(parts[i]);
            }
            VectorPoint p = new VectorPoint(key, numbers);
            p.addCap(cap);
            return p;
        }
        return null;
    }

    public static List<VectorPoint> readVectors(File file, int startIndex, int endIndex) {
        List<VectorPoint> vecs = new ArrayList<VectorPoint>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            int count = 0;
            int readCount = 0;
            int globalVectorLength = -1;
            while ((line = br.readLine()) != null) {
                if (count >= startIndex) {
                    readCount++;
                    // process the line.
                    String parts[] = line.trim().split(" ");
                    if (parts.length > 0 && !(parts.length == 1 && parts[0].equals(""))) {
                        int key = Integer.parseInt(parts[0]);
                        double cap = Double.parseDouble(parts[1]);

                        int vectorLength = parts.length - 2;
                        double[] numbers = new double[vectorLength];
                        for (int i = 2; i < parts.length; i++) {
                            numbers[i - 2] = Double.parseDouble(parts[i]);
                        }
                        VectorPoint p = new VectorPoint(key, numbers);
                        if (key < 10) {
                            p = new VectorPoint(key, globalVectorLength, true);
                            p.setConstantVector(true);
                        } else if (globalVectorLength < 0){
                            globalVectorLength = vectorLength;
                        }
                        p.addCap(cap);
                        vecs.add(p);
                    }

                }
                count++;
                // we stop
                if (readCount > endIndex - startIndex) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignore) {
                }
            }
        }
        return vecs;
    }

    public static List<Integer> readVectorKeys(File file) {
        List<Integer> keys = new ArrayList<Integer>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                // process the line.
                String parts[] = line.trim().split(" ");
                if (parts.length > 0 && !(parts.length == 1 && parts[0].equals(""))) {
                    int key = Integer.parseInt(parts[0]);
                    keys.add(key);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignore) {
                }
            }
        }
        return keys;
    }

    public static Point readPoint(String line) throws Exception {
        try {
            String[] splits = line.split("\t");

            int i = Integer.parseInt(splits[0]);
            double x = Double.parseDouble(splits[1]);
            double y = Double.parseDouble(splits[2]);
            double z = Double.parseDouble(splits[3]);
            int clazz = Integer.parseInt(splits[4]);

            return new Point(i, x, y, z, clazz);
        } catch (NumberFormatException e) {
            throw new Exception(e);
        }
    }

    public static SectorRecord readSectorRecord(String line) {
        String []splits = line.split("\",\"");
        return new SectorRecord(splits[5].replaceAll("^\"|\"$", ""), splits[0].replaceAll("^\"|\"$", ""));
    }

    public static Bin readBin(String line) {
        String []parts = line.split(",");
        double start = Double.parseDouble(parts[1]);
        double end = Double.parseDouble(parts[2]);
        Bin bin = new Bin();
        bin.start = start;
        bin.end = end;
        for (int i = 3; i < parts.length; i++) {
            bin.symbols.add(parts[i]);
        }
        return bin;
    }

    // first read the original stock file and load the mapping from permno to stock symbol
    public static Map<Integer, String> loadMapping(String inFile) {
        System.out.println("Reading original stock file: " + inFile);
        BufferedReader bufRead = null;
        Map<Integer, String> maps = new HashMap<Integer, String>();
        try {
            FileReader input = new FileReader(inFile);
            bufRead = new BufferedReader(input);

            Record record;
            while ((record = Utils.parseFile(bufRead)) != null) {
                maps.put(record.getSymbol(), record.getSymbolString());
            }
            maps.put(0, "CONST");
            maps.put(1, "10INC");
            maps.put(2, "20INC");
            maps.put(3, "10DEC");
            maps.put(4, "20DEC");
            System.out.println("No of stocks: " + maps.size());
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to open file");
        }
        return maps;
    }

    public static short[][] readRowRange(String fname, Range rows, int globalColCount, ByteOrder endianness){
        try (FileChannel fc = (FileChannel) Files
                .newByteChannel(Paths.get(fname), StandardOpenOption.READ)) {
            int dataTypeSize = Short.BYTES;
            long pos = ((long) rows.getStartIndex()) * globalColCount *
                    dataTypeSize;
            MappedByteBuffer mappedBytes = fc.map(
                    FileChannel.MapMode.READ_ONLY, pos,
                    rows.getLength() * globalColCount * dataTypeSize);
            mappedBytes.order(endianness);

            int rowCount = rows.getLength();
            short[][] rowBlock = new short[rowCount][];
            for (int i = 0; i < rowCount; ++i){
                short [] rowBlockRow = rowBlock[i] = new short[globalColCount];
                for (int j = 0; j < globalColCount; ++j){
                    int procLocalPnum =  i * globalColCount + j;
                    int bytePosition = procLocalPnum * dataTypeSize;
                    short tmp = mappedBytes.getShort(bytePosition);
                    // -1.0 indicates missing values
                    rowBlockRow[j] = tmp;
                }
            }
            return rowBlock;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> genDateList(Date startDate, Date endDate, int mode) {
        TreeMap<String, List<Date>> dates = new TreeMap<String, List<Date>>();
        List<String> dateList = new ArrayList<String>();
        Date currentDate = startDate;
        if (mode == 1) {
            // month data
            while (currentDate.before(endDate)) {
                List<Date> d = new ArrayList<Date>();
                d.add(currentDate);
                dates.put(getMonthString(currentDate), d);
                currentDate = addMonth(currentDate);
            }
        } else if (mode == 2) {
            while (currentDate.before(endDate)) {
                String startName = getMonthString(currentDate);
                Date tempDate = currentDate;
                List<Date> d = new ArrayList<Date>();
                for (int i = 0; i < 12; i++) {
                    d.add(tempDate);
                    tempDate = addMonth(tempDate);
                }
                currentDate = tempDate;
                String endDateName = getMonthString(tempDate);
                dates.put(startName + "_" + endDateName, d);
            }
        } else if (mode == 3) {
            List<Date> d = new ArrayList<Date>();
            while (currentDate.before(endDate)) {
                d.add(currentDate);
                currentDate = addMonth(currentDate);
            }
            dates.put(getMonthString(startDate) + "_" + getMonthString(endDate), d);
        } else if (mode == 4) {
            while (currentDate.before(endDate)) {
                String startName = getMonthString(currentDate);
                Date tempDate = currentDate;
                List<Date> d = new ArrayList<Date>();
                for (int i = 0; i < 12; i++) {
                    d.add(tempDate);
                    tempDate = addMonth(tempDate);
                }
                currentDate = addMonth(currentDate);
                String endDateName = getMonthString(tempDate);
                String key = startName + "_" + endDateName;
                dates.put(key, d);
                dateList.add(key);
                if (!tempDate.before(endDate)) {
                    break;
                }
            }
        } else if (mode == 5) {
            Date lastDate;
            do {
                lastDate = addYear(currentDate);
                String start = getDateString(currentDate);
                String end = getDateString(lastDate);
                List<Date> list = new ArrayList<Date>();
                list.add(currentDate);
                list.add(lastDate);

                currentDate = addDays(currentDate, 7);
                String key = start + "_" + end;
                dates.put(key, list);
                dateList.add(key);
            } while (lastDate.before(endDate));
        } else if (mode == 6) {
            Date lastDate;
            do {
                lastDate = addYear(currentDate);
                String start = getDateString(currentDate);
                String end = getDateString(lastDate);
                List<Date> list = new ArrayList<Date>();
                list.add(currentDate);
                list.add(lastDate);

                currentDate = addDays(currentDate, 1);
                String key = start + "_" + end;
                dates.put(key, list);
                dateList.add(key);
            } while (lastDate.before(endDate));
        } else if (mode == 7) {
            Date lastDate = addYear(currentDate);;
            do {
                String start = getDateString(currentDate);
                String end = getDateString(lastDate);
                List<Date> list = new ArrayList<Date>();
                list.add(currentDate);
                list.add(lastDate);

                lastDate = addDays(lastDate, 7);
                String key = start + "_" + end;
                dates.put(key, list);
                dateList.add(key);
            } while (lastDate.before(endDate));
        }
        return dateList;
    }

    public static TreeMap<String, List<Date>> genDates(Date startDate, Date endDate, int mode) {
        TreeMap<String, List<Date>> dates = new TreeMap<String, List<Date>>();
        Date currentDate = startDate;
        if (mode == 1) {
            // month data
            while (currentDate.before(endDate)) {
                List<Date> d = new ArrayList<Date>();
                d.add(currentDate);
                dates.put(getMonthString(currentDate), d);
                currentDate = addMonth(currentDate);
            }
        } else if (mode == 2) {
            while (currentDate.before(endDate)) {
                String startName = getMonthString(currentDate);
                Date tempDate = currentDate;
                List<Date> d = new ArrayList<Date>();
                for (int i = 0; i < 12; i++) {
                    d.add(tempDate);
                    tempDate = addMonth(tempDate);
                }
                currentDate = tempDate;
                String endDateName = getMonthString(tempDate);
                dates.put(startName + "_" + endDateName, d);
            }
        } else if (mode == 3) {
            List<Date> d = new ArrayList<Date>();
            while (currentDate.before(endDate)) {
                d.add(currentDate);
                currentDate = addMonth(currentDate);
            }
            dates.put(getMonthString(startDate) + "_" + getMonthString(endDate), d);
        } else if (mode == 4) {
            while (currentDate.before(endDate)) {
                String startName = getMonthString(currentDate);
                Date tempDate = currentDate;
                List<Date> d = new ArrayList<Date>();
                for (int i = 0; i < 12; i++) {
                    d.add(tempDate);
                    tempDate = addMonth(tempDate);
                }
                currentDate = addMonth(currentDate);
                String endDateName = getMonthString(tempDate);
                dates.put(startName + "_" + endDateName, d);
                if (!tempDate.before(endDate)) {
                    break;
                }
            }
        } else if (mode == 5) {
            Date lastDate;
            do {
                lastDate = addYear(currentDate);
                String start = getDateString(currentDate);
                String end = getDateString(lastDate);
                List<Date> list = new ArrayList<Date>();
                list.add(currentDate);
                list.add(lastDate);

                currentDate = addDays(currentDate, 7);
                dates.put(start + "_" + end, list);
            } while (lastDate.before(endDate));
        } else if (mode == 6) {
            Date lastDate;
            do {
                lastDate = addYear(currentDate);
                String start = getDateString(currentDate);
                String end = getDateString(lastDate);
                List<Date> list = new ArrayList<Date>();
                list.add(currentDate);
                list.add(lastDate);

                currentDate = addDays(currentDate, 1);
                dates.put(start + "_" + end, list);
            } while (lastDate.before(endDate));
        } else if (mode == 7) {
            Date lastDate = addYear(currentDate);;
            do {
                String start = getDateString(currentDate);
                String end = getDateString(lastDate);
                List<Date> list = new ArrayList<Date>();
                list.add(currentDate);
                list.add(lastDate);

                lastDate = addDays(lastDate, 7);
                dates.put(start + "_" + end, list);
            } while (lastDate.before(endDate));
        }
        return dates;
    }

    public static Option createOption(String opt, boolean hasArg, String description, boolean required) {
        Option symbolListOption = new Option(opt, hasArg, description);
        symbolListOption.setRequired(required);
        return symbolListOption;
    }

    /**
     * Load the mapping from permno to point
     * @param pointFile the point file
     * @param keys keys
     * @return map
     */
    public static Map<Integer, Point> loadPoints(File pointFile, List<Integer> keys) {
        BufferedReader bufRead = null;
        Map<Integer, Point> points = new HashMap<Integer, Point>();
        try {
            bufRead = new BufferedReader(new FileReader(pointFile));
            String inputLine;
            int index = 0;
            while ((inputLine = bufRead.readLine()) != null) {
                Point p = readPoint(inputLine);
                points.put(keys.get(index), p);
                if (keys.get(index) == 0) {
                    System.out.println("Read key 0");
                }
                index++;
            }
            if (index != keys.size()) {
                throw new RuntimeException("Keys are not read fully: " + index +  " != " + keys.size() + " " + pointFile.getAbsolutePath());
            }
            return points;
        } catch (Exception e) {
            throw new RuntimeException("Faile to read file: " + pointFile.getAbsolutePath(), e);
        }
    }

    /**
     * Load the mapping from permno to point
     * @param vectorFile the vector file
     * @return map
     */
    public static Map<Integer, Double> loadCaps(File vectorFile) {
        BufferedReader bufRead = null;
        Map<Integer, Double> points = new HashMap<Integer, Double>();
        try {
            bufRead = new BufferedReader(new FileReader(vectorFile));
            String inputLine;
            int index = 0;
            while ((inputLine = bufRead.readLine()) != null) {
                VectorPoint p = parseVectorLine(inputLine);
                points.put(p.getKey(), p.getTotalCap());
                index++;
            }
            return points;
        } catch (IOException e) {
            throw new RuntimeException("Faile to read file: " + vectorFile.getAbsolutePath());
        }
    }

    public static void savePlotViz(String outFileName, Plotviz plotviz) throws FileNotFoundException, JAXBException {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(outFileName);
            JAXBContext ctx = JAXBContext.newInstance(Plotviz.class);
            Marshaller ma = ctx.createMarshaller();
            ma.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            ma.marshal(plotviz, fileOutputStream);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            TreeMap<String, List<Date>> dates = genDates(formatter.parse("20040101"), formatter.parse("20050130"), 6);
//            for (Map.Entry<String, List<Date>> e : dates.entrySet()) {
//                System.out.println(e.getKey());
//            }

            List<String> datesList = genDateList(formatter.parse("20040101"), formatter.parse("20050130"), 7);
            for (String s : datesList) {
                System.out.println(s);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
