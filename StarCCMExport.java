package com.inkloyd;

import java.io.File;
import java.util.*;
import java.io.*;
import star.common.*;
import star.base.report.*;
import star.vis.*;

public class StarCCMExport extends StarMacro {
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
                    thisPlot.export(resolvePath(basePath + "Plots_" + thisPlot.getPresentationName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".csv"), ",");
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
