import java.io.File;
import java.util.*;
import java.io.*;
import star.common.*;
import star.base.report.*;
import star.vis.*;

public class StarCCMExport extends StarMacro {
    public void SortCSV(String csvPath, String simName, String reportName){
        try{
            BufferedReader br = new BufferedReader(new FileReader(csvPath));
            String line;
            Map<String, String> map = new TreeMap<>();

            while((line=br.readLine())!=null){
                String str[] = line.split(",");
                map.put(str[0], str[1]);
            }
            br.close();

            // File path generator
            String newCsvPath = simName + reportName;

            BufferedWriter bw = new BufferedWriter(new FileWriter(newCsvPath));
            for (Map.Entry<String, String> entry : map.entrySet()){
                bw.append(entry.getKey()).append(',').append(entry.getValue()).append(System.getProperty("line.separator"));
            }
            bw.close();
        } catch (Exception ex) {
            System.out.println(ex.toString());
        } finally {

        }
    }

    public void execute() {

        Simulation simFile = getActiveSimulation();

        try {
            String simName = simFile.getPresentationName();
            String curDir = System.getProperty("user.dir");
            boolean exists = (new File(simName)).exists();
            boolean success;
            if (!exists) {
                success = (new File(simName)).mkdir();
                if (!success) {
                    simFile.println("Folder creation failed.");
                }
            }

            simFile.println("Simulation Name:" + simName);

            String basePath = (curDir + File.separatorChar + simName + File.separatorChar);

            // Export all reports
            Collection<Report> reportCollection = simFile.getReportManager().getObjects();

            if (reportCollection.isEmpty()) {
                simFile.println("No reports in current simulation.");
            }
            else {
                simFile.println("Report directory: " + basePath);

                BufferedWriter reportWriter = new BufferedWriter(new FileWriter(resolvePath(basePath + "Reports.csv")));
                reportWriter.write("Report Name,Value,Unit,\n");

                for (Report thisReport : reportCollection) {
                    String fieldLocationName = thisReport.getPresentationName();
                    Double fieldValue = thisReport.getReportMonitorValue();
                    String fieldUnits = thisReport.getUnits().toString();
                    simFile.println("Field Location: " + fieldLocationName);
                    reportWriter.write(fieldLocationName + "," + fieldValue + "," + fieldUnits + "\n");
                }
                reportWriter.close();
            }

            // Export all plots
            Collection<StarPlot> plotCollection = simFile.getPlotManager().getObjects();

            if (plotCollection.isEmpty()) {
                simFile.println("No plots in current simulation.");
            }
            else {
                simFile.println("Plot directory: " + basePath);

                for (StarPlot thisPlot : plotCollection) {
                    simFile.println("Plot Name: " + thisPlot.getPresentationName());
                    String csvPath = resolvePath(basePath + "Plots_" + thisPlot.getPresentationName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".csv");
                    thisPlot.export(csvPath, ",");
                    SortCSV(csvPath, simName, thisPlot.getPresentationName());
                }
            }
            
            // Export all scenes
            Collection<Scene> sceneCollection = simFile.getSceneManager().getScenes();

            if (sceneCollection.isEmpty()) {
                simFile.println("No scenes in current simulation.");
            }
            else {
                simFile.println("Scene directory: " + basePath);

                int xResolution   = 1920;
                int yResolution   = 1080;
                int magnification = 1;

                for (Scene thisScene : sceneCollection) {
                    String curScene = thisScene.toString().replaceAll("[^a-zA-Z0-9.-]", "_");
                    simFile.getSceneManager().getSceneByName(curScene);
                    thisScene.printAndWait(resolvePath(basePath + "Scenes_" + curScene + ".png"), magnification, xResolution, yResolution);
                }
            }
        }
        catch (IOException iOException) {
            simFile.println(iOException);
        }
    }
}
