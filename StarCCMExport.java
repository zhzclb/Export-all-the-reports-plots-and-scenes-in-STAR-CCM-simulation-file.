import java.io.File;
import java.util.*;
import java.io.*;

import star.common.*;
import star.base.report.*;
import star.vis.*;

public class StarCCMExport extends StarMacro
{
    private static String curDir = System.getProperty( "user.dir" );
    
    
    /**
     * @param simName should be like this "case-a_1-std-lin"
     * @param objName should be like this "_XBT_y-t"
     * @return "../data/csv/case-a/xbt/y-t/1-std-lin.csv"
     */
    private String[] ParseCaseName( String simName, String objName )
    {
        String elemSim[] = simName.split( "_" );
        String caseName = elemSim[0];
        String modelName = elemSim[1];
        
        String elemRep[] = objName.split( "_" );
        String locName = elemRep[1];
        String grpName = elemRep[2];
        
        System.out.println( caseName + modelName + locName + grpName );
        
        return new String[]{ curDir + File.separatorChar
                + "data" + File.separatorChar
                + caseName + File.separatorChar
                + locName + File.separatorChar
                + grpName,
                modelName };
    }
    
    private String MakeFolders( String folderName )
    {
        try
        {
            File f = new File( folderName );
            if( !f.isDirectory() )
                f.mkdirs();
            else
                System.out.println( "Path already exists: " + f.getPath() );
        }
        catch ( Exception ex )
        {
            System.out.println( ex.toString() );
        }
        
        return folderName;
    }
    
    private void SortCSV( String filePath, String simName, String reportName )
    {
        try
        {
            int lineNum = 0;
            BufferedReader br = new BufferedReader( new FileReader( filePath ) );
            String line;
            Map< Double, Double > map = new TreeMap<>();
            
            while( ( line = br.readLine() ) != null )
            {
                if( lineNum != 0 )
                {
                    String str[] = line.split( "," );
                    map.put( Double.parseDouble( str[0] ), Double.parseDouble( str[1] ) );
                }
                lineNum++;
            }
            br.close();
            
            String foldName = MakeFolders( ParseCaseName( simName, reportName )[0] ) + File.separatorChar;
            String fileName = ParseCaseName( simName, reportName )[1] + ".csv";
            
            BufferedWriter bw = new BufferedWriter( new FileWriter( foldName + fileName ) );
            for( Map.Entry< Double, Double > entry : map.entrySet() )
            {
                bw.append( String.format( "%.4f", entry.getKey() ) ).append( "," )
                  .append( String.format( "%.4f", entry.getValue() ) )
                  .append( System.getProperty( "line.separator" ) );
            }
            bw.close();
        }
        catch ( Exception ex )
        {
            System.out.println( ex.toString() );
        }
    }
    
    public void execute()
    {
        Simulation simFile = getActiveSimulation();
        String simName = simFile.getPresentationName();
        
        try
        {
            simFile.println( "No plots in current simulation." );
            boolean exists = ( new File( simName ) ).exists();
            boolean success;
            if( !exists )
            {
                success = ( new File( simName ) ).mkdir();
                if( !success )
                {
                    simFile.println( "Folder creation failed." );
                }
            }
            
            String basePath = ( curDir + File.separatorChar + simName + File.separatorChar );
            
            // Export all reports
            Collection< Report > reportCollection = simFile.getReportManager().getObjects();
            if( !reportCollection.isEmpty() )
            {
                BufferedWriter reportFile = new BufferedWriter( new FileWriter( resolvePath( basePath + "Reports.csv" ) ) );
                
                for( Report thisReport : reportCollection )
                {
                    String fieldNames = thisReport.getPresentationName();
                    Double fieldValue = thisReport.getReportMonitorValue();
                    String fieldUnits = thisReport.getUnits().toString();
                    reportFile.append( fieldNames ).append( "," )
                              .append( fieldValue.toString() ).append( "," )
                              .append( fieldUnits ).append( System.getProperty( "line.separator" ) );
                }
                reportFile.close();
            }
            
            // Export all plots
            Collection< StarPlot > plotCollection = simFile.getPlotManager().getObjects();
            if( !plotCollection.isEmpty() )
            {
                for( StarPlot thisPlot : plotCollection )
                {
                    String plotName = thisPlot.getPresentationName();
                    if( !plotName.contains( "residuals" ) )
                    {
                        String csvPath = resolvePath( basePath + plotName + ".csv" );
                        thisPlot.export( csvPath, "," );
                        SortCSV( csvPath, simName, plotName );
                        new File( csvPath ).delete();
                    }
                }
            }
            
            // Export all scenes
            Collection< Scene > sceneCollection = simFile.getSceneManager().getScenes();
            if( !sceneCollection.isEmpty() )
            {
                int xResolution = 1920;
                int yResolution = 1080;
                int magnification = 1;
    
                for( Scene thisScene : sceneCollection )
                {
                    String curScene = thisScene.toString();
        
                    if( curScene.contains( "contour" ) )
                    {
                        String foldName = MakeFolders( ParseCaseName( simName, curScene )[0] );
                        String fileName = ParseCaseName( simName, curScene )[1] + ".png";
                        simFile.getSceneManager().getSceneByName( curScene );
                        thisScene.printAndWait( resolvePath( foldName + File.separatorChar + fileName ),
                                magnification, xResolution, yResolution );
                    }
                }
            }
            
            new File( basePath ).delete();
        }
        catch ( Exception ex )
        {
            System.out.println( ex.toString() );
        }
    }
}
