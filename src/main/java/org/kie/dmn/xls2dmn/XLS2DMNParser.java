package org.kie.dmn.xls2dmn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.drools.decisiontable.parser.DecisionTableParser;
import org.drools.template.parser.DecisionTableParseException;
import org.kie.dmn.api.marshalling.DMNMarshaller;
import org.kie.dmn.backend.marshalling.v1x.DMNMarshallerFactory;
import org.kie.dmn.feel.codegen.feel11.CodegenStringUtil;
import org.kie.dmn.model.api.Definitions;
import org.kie.dmn.model.api.InformationItem;
import org.kie.dmn.model.api.InputData;
import org.kie.dmn.model.v1_3.TInformationItem;
import org.kie.dmn.model.v1_3.TDefinitions;
import org.kie.dmn.model.v1_3.TInputData;

public class XLS2DMNParser implements DecisionTableParser {

    @Override
    public void parseFile(InputStream inStream) {
        try {
            parseWorkbook(WorkbookFactory.create(inStream));
        } catch (IOException e) {
            throw new DecisionTableParseException(
                    "Failed to open Excel stream, " + "please check that the content is xls97 format.", e);
        }
    }

    @Override
    public void parseFile(File file) {
        try {
            parseWorkbook(WorkbookFactory.create(file, (String) null, true));
        } catch (IOException e) {
            throw new DecisionTableParseException(
                    "Failed to open Excel stream, " + "please check that the content is xls97 format.", e);
        }
    }

    public void parseWorkbook(Workbook workbook) {
        Map<String, List<String>> overview = new HashMap<>();
        DataFormatter formatter = new DataFormatter();
        for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
            Sheet sheet = workbook.getSheetAt(s);
            int maxRows = sheet.getLastRowNum();

            for (int i = 0; i <= maxRows; i++) {
                Row row = sheet.getRow(i);
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
                break; // header found.
            }
        }
        overview.entrySet().forEach(System.out::println);
        Map<String, DTHeaderInfo> headerInfos = generateDTHeaderInfo(overview);
        headerInfos.entrySet().forEach(System.out::println);
        Definitions definitions = new TDefinitions();
        setDefaultNSContext(definitions);
        definitions.setName("xls2dmn");
        String namespace = "xls2dmn_"+UUID.randomUUID();
        definitions.setNamespace(namespace);
        definitions.getNsContext().put(XMLConstants.DEFAULT_NS_PREFIX, namespace);
        definitions.setExporter("kie-dmn-xls2dmn");
        appendInputData(definitions, headerInfos);
        DMNMarshaller dmnMarshaller = DMNMarshallerFactory.newDefaultMarshaller();
        String xml = dmnMarshaller.marshal(definitions);
        System.out.println(xml);
    }

    private void setDefaultNSContext(Definitions definitions) {
        Map<String, String> nsContext = definitions.getNsContext();
        nsContext.put("feel", org.kie.dmn.model.v1_3.KieDMNModelInstrumentedBase.URI_FEEL);
        nsContext.put("dmn", org.kie.dmn.model.v1_3.KieDMNModelInstrumentedBase.URI_DMN);
        nsContext.put("dmndi", org.kie.dmn.model.v1_3.KieDMNModelInstrumentedBase.URI_DMNDI);
        nsContext.put("di", org.kie.dmn.model.v1_3.KieDMNModelInstrumentedBase.URI_DI);
        nsContext.put("dc", org.kie.dmn.model.v1_3.KieDMNModelInstrumentedBase.URI_DC);
    }

    private void appendInputData(Definitions definitions, Map<String, DTHeaderInfo> headerInfos) {
        for ( DTHeaderInfo hi : headerInfos.values()) {
            for(String ri : hi.getRequiredInput()) {
                InputData id = new TInputData();
                id.setName(ri);
                id.setId("id_"+CodegenStringUtil.escapeIdentifier(ri));
                InformationItem variable = new TInformationItem();
                variable.setName(ri);
                variable.setId("idvar_"+CodegenStringUtil.escapeIdentifier(ri));
                id.setVariable(variable);
                definitions.getDrgElement().add(id);
            }
        }
    }

    private Map<String, DTHeaderInfo> generateDTHeaderInfo(Map<String, List<String>> overview) {
        Map<String, DTHeaderInfo> result = new HashMap<>();
        for (Entry<String, List<String>> kv : overview.entrySet()) {
            String sheetName = kv.getKey();
            List<String> requiredInput = new ArrayList<>();
            List<String> requiredDecision = new ArrayList<>();
            int hIndex = kv.getValue().indexOf(sheetName);
            if (hIndex < 0) {
                throw new RuntimeException("There is no result output column in sheet: " + sheetName);
            }
            if (hIndex != kv.getValue().size()) {
                for (int i = hIndex+1; i < kv.getValue().size(); i++) {
                    String afterIndexValue = kv.getValue().get(i);
                    if (!(afterIndexValue == null || afterIndexValue.isEmpty())) {
                        throw new RuntimeException("Decision name was not last, on the right I found "+afterIndexValue);
                    }
                }
            }
            for (int i = 0; i < hIndex; i++) {
                String hValue = kv.getValue().get(i);
                if (overview.containsKey(hValue)) {
                    requiredDecision.add(hValue);
                } else {
                    requiredInput.add(hValue);
                }
            }
            DTHeaderInfo info = new DTHeaderInfo(sheetName, kv.getValue(), hIndex, requiredInput, requiredDecision);
            result.put(sheetName, info);
        }
        return result;
    }
    
}