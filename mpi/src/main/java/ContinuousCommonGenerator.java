import mpi.MPI;
import mpi.MPIException;
import mpi.MpiOps;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.*;

public class ContinuousCommonGenerator {
    private String pointFolder;
    private String vectorFolder;
    private String destFolder;
    private String symbolList;
    private String stocksFile;
    private Date startDate;
    private Date endDate;
    private boolean mpi;
    private int mode = 4;

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption(Utils.createOption("sf", true, "The complete stock file", false));
        options.addOption("v", true, "Input Vector folder");
        options.addOption("p", true, "Points folder");
        options.addOption("d", true, "Destination point folder");
        options.addOption(Utils.createOption("m", false, "MPI", false));
        options.addOption(Utils.createOption("s", true, "Symbol List", false));
        options.addOption(Utils.createOption("sd", true, "Start date", true));
        options.addOption(Utils.createOption("ed", true, "End date", true));
        options.addOption(Utils.createOption("md", true, "End date", false));

        CommandLineParser commandLineParser = new BasicParser();
        try {
            CommandLine cmd = commandLineParser.parse(options, args);
            String vectorFolder = cmd.getOptionValue("v");
            String pointsFolder = cmd.getOptionValue("p");
            String distFolder = cmd.getOptionValue("d");
            String symbolList = cmd.getOptionValue("s");
            String stockFile = cmd.getOptionValue("sf");
            String startDate = cmd.getOptionValue("sd");
            String endDate = cmd.getOptionValue("ed");
            String mode = cmd.getOptionValue("md");
            boolean mpi = cmd.hasOption("m");
            if (mpi) {
                MPI.Init(args);
            }
            ContinuousCommonGenerator pointTransformer = new ContinuousCommonGenerator(pointsFolder, vectorFolder,
                    distFolder, symbolList, stockFile, startDate, endDate, mpi);
            if (mode != null) {
                pointTransformer.setMode(Integer.parseInt(mode));
            }
            pointTransformer.process();
            if (mpi) {
                MPI.Finalize();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (MPIException e) {
            e.printStackTrace();
        }
    }

    public ContinuousCommonGenerator(String pointFolder, String vectorFolder,
                            String destPointFolder, String symbolList,
                            String stockFile, String startDate, String endDate, boolean mpi) {
        this.pointFolder = pointFolder;
        this.vectorFolder = vectorFolder;
        this.destFolder = destPointFolder;
        this.symbolList = symbolList;
        this.stocksFile = stockFile;
        this.startDate = Utils.parseDateString(startDate);
        this.endDate = Utils.parseDateString(endDate);
        this.mpi = mpi;

    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    private Queue<FilePair> filePairsPerProcess = new LinkedList<FilePair>();

    private class FilePair {
        String first;
        String second;

        private FilePair(String first, String second) {
            this.first = first;
            this.second = second;
        }
    }

    public void process() {
        List<String> dates = Utils.genDateList(this.startDate, this.endDate, mode);
        MpiOps mpiOps = null;
        StringBuilder sb = new StringBuilder();
        if (mpi) {
            try {
                mpiOps = new MpiOps();
                int rank = mpiOps.getRank();
                int size = mpiOps.getSize();
                sb.append("Rank: ").append(rank).append(" ");
                int j = 0;
                for (int i = 1; i < dates.size(); i++) {
                    String firstFile = dates.get(i - 1);
                    String secondFile = dates.get(i);
                    if (j == rank) {
                        this.filePairsPerProcess.add(new FilePair(firstFile, secondFile));
                        sb.append(firstFile).append(", ").append(secondFile).append("::");
                    }
                    j++;
                    if (j == size) {
                        j = 0;
                    }
                }
            } catch (MPIException e) {
                throw new RuntimeException("Failed to create MPI", e);
            }
        } else {
            for (int i = 1; i < dates.size(); i++) {
                String firstFile = dates.get(i - 1);
                String secondFile = dates.get(i);
                sb.append(firstFile).append(", ").append(secondFile).append("::");
                filePairsPerProcess.add(new FilePair(firstFile, secondFile));
            }
        }
        System.out.println("Assigned pairs: " + sb.toString());
        // process the files assigned
        for (FilePair f : filePairsPerProcess) {
            processPair(f.first, f.second);
        }
    }

    private void processPair(String firstFileWithoutExt, String secondfFileWithoutExt) {
        String firstFileName = firstFileWithoutExt + ".csv";
        String secondFileName = secondfFileWithoutExt + ".csv";

        String firstVectorFileName = this.vectorFolder + "/" + firstFileName;
        String secondVectorFileName = this.vectorFolder + "/" + secondFileName;
        String firstPointFileName = this.pointFolder + "/" + firstFileWithoutExt + ".txt";
        String secondPointFileName = this.pointFolder + "/" + secondfFileWithoutExt + ".txt";

        System.out.println("Processing pair: " + firstVectorFileName + " " + secondVectorFileName + " " + firstPointFileName + " " + secondPointFileName);

        // first lets read the vector keys
        List<Integer> firstVectorKeys = Utils.readVectorKeys(new File(firstVectorFileName));
        List<Integer> secondVectorKeys = Utils.readVectorKeys(new File(secondVectorFileName));

        // now lets get the common keys for both of them
        TreeSet<Integer> commonKeys = new TreeSet<Integer>(firstVectorKeys);
        commonKeys.retainAll(new TreeSet<Integer>(secondVectorKeys));
        System.out.println("Fits keys: " + firstVectorKeys.size() + " Second keys:"  + secondVectorKeys.size() + " Common keys: " + commonKeys.size());
        // now read the two point files
        Map<Integer, Point> firstPoints = Utils.loadPoints(new File(firstPointFileName), firstVectorKeys);
        Map<Integer, Point> secondPoints = Utils.loadPoints(new File(secondPointFileName), secondVectorKeys);
        Map<Integer, Double> secondWeights = Utils.loadCaps(new File(secondVectorFileName));
        // go through the common keys and write them out
        String pointOutFolder = destFolder + "/" + secondfFileWithoutExt + "/points";
        String weightOutFolder = destFolder + "/" + secondfFileWithoutExt + "/weights";

        new File(pointOutFolder).mkdirs();
        new File(weightOutFolder).mkdirs();

        String fistPointCommonFileName = pointOutFolder + "/first.txt";
        String secondPointCommonFileName = pointOutFolder + "/second.txt";
        String secondWeightFileName = weightOutFolder + "/" + "second.csv";

        writePoints(commonKeys, firstPoints, fistPointCommonFileName);
        writePoints(commonKeys, secondPoints, secondPointCommonFileName);
        writeWeights(commonKeys, secondWeightFileName, secondWeights);
    }

    private void writePoints(TreeSet<Integer> commonKeys, Map<Integer, Point> pointMap, String file) {
        BufferedWriter bufWriter = null;
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bufWriter = new BufferedWriter(new OutputStreamWriter(fos));
            int i = 0;
            for (Integer key : commonKeys) {
                Point p = pointMap.get(key);
                if (p == null) {
                    System.out.println("Key cannot be found: " + key);
                }
                p.setIndex(i++);
                bufWriter.write(p.serialize());
                bufWriter.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file", e);
        } finally {
            if (bufWriter != null) {
                try {
                    bufWriter.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private void writeWeights(TreeSet<Integer> commonKeys, String weightFile, Map<Integer, Double> caps) {
        WriterWrapper weightWriter = null;
        try {
            double max = Double.MIN_VALUE;
            for (Double d : caps.values()) {
                if (max < d) {
                    max = d;
                }
            }
            weightWriter = new WriterWrapper(weightFile, true);
            int i = 0;
            for (Integer key : commonKeys) {
                weightWriter.write(caps.get(key) / max);
                weightWriter.line();
            }
        } finally {
            if (weightWriter != null) {
                weightWriter.close();
            }
        }
    }
}
