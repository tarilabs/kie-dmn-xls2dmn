package org.kie.dmn.xls2dmn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.drools.decisiontable.parser.DecisionTableParser;
import org.drools.template.parser.DecisionTableParseException;

public class ExcelParser implements DecisionTableParser {

    @Override
    public void parseFile( InputStream inStream ) {
        try {
            parseWorkbook( WorkbookFactory.create( inStream ) );
        } catch ( IOException e ) {
            throw new DecisionTableParseException( "Failed to open Excel stream, " + "please check that the content is xls97 format.",
                                                   e );
        }
    }

    @Override
    public void parseFile( File file ) {
        try {
            parseWorkbook( WorkbookFactory.create( file, (String)null, true ) );
        } catch ( IOException e ) {
            throw new DecisionTableParseException( "Failed to open Excel stream, " + "please check that the content is xls97 format.",
                                                   e );
        }
    }

    public void parseWorkbook( Workbook workbook ) {
        Map<String, List<String>> overview = new HashMap<>();
        DataFormatter formatter = new DataFormatter();
        for (int s = 0; s < workbook.getNumberOfSheets(); s++)  {
            Sheet sheet = workbook.getSheetAt(s);
            int maxRows = sheet.getLastRowNum();
    
            for ( int i = 0; i <= maxRows; i++ ) {
                Row row = sheet.getRow( i );
                int lastCellNum = row != null ? row.getLastCellNum() : 0;
                if (lastCellNum == 0) {
                    continue; // skip empty row.
                }
                List<String> header = new ArrayList<>();
                for (Cell c : row) {
                    String text = formatter.formatCellValue(c);
                    header.add(text);
                }
                overview.put(sheet.getSheetName(), header);
                break;
            }
        }
        overview.entrySet().forEach(System.out::println);
    }
    
}