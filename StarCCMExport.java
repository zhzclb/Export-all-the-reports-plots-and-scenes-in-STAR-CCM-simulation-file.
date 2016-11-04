import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;

import star.common.*;
import star.base.report.*;
import star.vis.*;

import static java.lang.Double.parseDouble;

public class StarCCMExport extends StarMacro
{
    private static String curDir = System.getProperty( "user.dir" );
    
    private String[] ParseCaseName( String simName )
    {
        String elemSim[] = simName.split( "_" );
        String caseName = elemSim[0];
        String modelName = elemSim[1];
    
        System.out.println( caseName + modelName );
    
        MakeFolders(curDir + File.separatorChar
                + "data" + File.separatorChar
                + "reports");
        
        return new String[]{ curDir + File.separatorChar
                + "data" + File.separatorChar
                + "reports",
                modelName };
    }
    
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
                    map.put( parseDouble( str[0] ), parseDouble( str[1] ) );
                }
                lineNum++;
            }
            br.close();
            
            String foldName = MakeFolders( ParseCaseName( simName, reportName )[0] ) + File.separatorChar;
            String fileName = ParseCaseName( simName, reportName )[1] + ".csv";
            
            BufferedWriter bw = new BufferedWriter( new FileWriter( foldName + fileName ) );
            for( Map.Entry< Double, Double > entry : map.entrySet() )
            {
                bw.append( String.format( "%.8f", entry.getKey() ) ).append( "," )
                        .append( String.format( "%.8f", entry.getValue() ) )
                        .append( System.getProperty( "line.separator" ) );
            }
            bw.close();
        }
        catch ( Exception ex )
        {
            System.out.println( ex.toString() );
        }
    }
    
    private void SortWireCSV( String paths[], String simName, String reportName )
    {
        TreeMap< Double, Double > map = new TreeMap<>();
        
        try
        {
            for( String path : paths )
            {
                BufferedReader br = new BufferedReader( new FileReader( path ) );
                String line;
                int lineNum = 0;
                while( ( line = br.readLine() ) != null )
                {
                    if( lineNum != 0 )
                    {
                        String str[] = line.split( "," );
                        if( !Objects.equals( str[0], "239.48811438446003" ) )
                        {
                            Double angle = parseDouble( str[0] );
                            Double value = parseDouble( str[1] );
                            map.put( angle, value );
                        }
                    }
                    lineNum++;
                }
                br.close();
            }
            
            String foldName = MakeFolders( ParseCaseName( simName, reportName )[0] ) + File.separatorChar;
            String fileName = ParseCaseName( simName, reportName )[1] + ".csv";
            
            BufferedWriter bw = new BufferedWriter( new FileWriter( foldName + fileName ) );
            for( Map.Entry< Double, Double > entry : map.entrySet() )
            {
                Double key = entry.getKey();
                double value = entry.getValue();
                
                if( key < 250 || key > 290 )
                {
                    bw.append( String.format( "%.8f", key ) ).append( "," )
                            .append( String.format( "%.8f", value ) )
                            .append( System.getProperty( "line.separator" ) );
                }
            }
            // Add first key value pair
            bw.append( String.format( "%.8f", map.firstEntry().getKey() ) ).append( "," )
                    .append( String.format( "%.8f", map.firstEntry().getValue() ) )
                    .append( System.getProperty( "line.separator" ) );
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
            
            // Export all plots
            Set< String > properties = new HashSet<>();
            
            Collection< StarPlot > plotCollection = simFile.getPlotManager().getObjects();
            if( !plotCollection.isEmpty() )
            {
                for( StarPlot thisPlot : plotCollection )
                {
                    String plotName = thisPlot.getPresentationName();
                    if( !plotName.contains( "residuals" ) && !plotName.contains( "wire" ) )
                    {
                        String csvPath = resolvePath( basePath + plotName + ".csv" );
                        thisPlot.export( csvPath, "," );
                        SortCSV( csvPath, simName, plotName );
                        new File( csvPath ).delete();
                    }
                    else
                    {
                        if( plotName.contains( "wire" ) )
                        {
                            String prop[] = plotName.split( "_" );
                            properties.add( prop[2] );
                            String csvPath = resolvePath( basePath + plotName + ".csv" );
                            thisPlot.export( csvPath, "," );
                        }
                    }
                }
            }
            
            for( String names : properties )
            {
                String paths[] = {
                        resolvePath( basePath + "_wire-inlet_" + names + ".csv" ),
                        resolvePath( basePath + "_wire-outlet_" + names + ".csv" )
                };
                
                String pName = "_wire_" + names;
                SortWireCSV( paths, simName, pName );
                new File( paths[0] ).delete();
                new File( paths[1] ).delete();
            }
    
            // Export all reports
            Collection< Report > reportCollection = simFile.getReportManager().getObjects();
            if( !reportCollection.isEmpty() )
            {
                String fileName = ParseCaseName( simName )[0] + File.separatorChar + ParseCaseName( simName )[1] + ".csv";
                BufferedWriter reportFile = new BufferedWriter( new FileWriter( resolvePath( fileName ) ) );
        
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
                        
                        Image src = ImageIO.read( new File( foldName + File.separatorChar + fileName ) );
                        int x = 90, y = 354, w = 1778, h = 283;
                        BufferedImage dst = new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB );
                        dst.getGraphics().drawImage( src, 0, 0, w, h, x, y, x + w, y + h, null );
                        ImageIO.write( dst, "png", new File( foldName + File.separatorChar + "_cropped_" + fileName + ".png" ) );
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