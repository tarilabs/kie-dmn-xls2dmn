package org.kie.dmn.xls2dmn;

import java.util.ArrayList;
import java.util.List;

import org.drools.template.parser.DataListener;
import org.kie.dmn.model.api.DecisionRule;
import org.kie.dmn.model.api.DecisionTable;
import org.kie.dmn.model.api.LiteralExpression;
import org.kie.dmn.model.api.UnaryTests;
import org.kie.dmn.model.v1_3.TDecisionRule;
import org.kie.dmn.model.v1_3.TLiteralExpression;
import org.kie.dmn.model.v1_3.TUnaryTests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DTSheetListener implements DataListener {

    private static final Logger LOG = LoggerFactory.getLogger(DTSheetListener.class);

    private final DecisionTable dt;
    private final DTHeaderInfo headerInfo;
    private DecisionRule curRule;

    public DTSheetListener(DecisionTable dt, DTHeaderInfo headerInfo) {
        this.dt = dt;
        this.headerInfo = headerInfo;
    }

    @Override
    public void startSheet(String name) {
        // nothing to do.
    }

    @Override
    public void finishSheet() {
        List<DecisionRule> toBeRemoved = new ArrayList<>();
        for (int i = 0; i < dt.getRule().size(); i++) {
            DecisionRule rule = dt.getRule().get(i);
            if (rule.getInputEntry().isEmpty()) {
                toBeRemoved.add(rule);
            }
        }
        dt.getRule().removeAll(toBeRemoved);
    }

    @Override
    public void newRow(int rowNumber, int columns) {
        if (rowNumber == 0) {
            return; // TODO row 0 being the header.
        }
        if (columns == -1) {
            curRule = null;
            return;
        }
        if (columns < dt.getInput().size() + dt.getOutput().size()) {
            throw new RuntimeException("In row " + rowNumber + " only total of cells is: " + columns);
        }
        curRule = new TDecisionRule();
        dt.getRule().add(curRule);
    }

    @Override
    public void newCell(int row, int column, String value, int mergedColStart) {
        if (row == 0) {
            return; // TODO row 0 being the header.
        }
        if (curRule == null) {
            return;
        }
        if (value == null || value.isEmpty()) {
            LOG.trace("ignoring row {}, col {} having value {}", row, column, value);
            return;
        }
        if (column < headerInfo.gethIndex()) {
            valueCheck(row, column, value);
            UnaryTests ut = new TUnaryTests();
            ut.setText(eValue(value));
            curRule.getInputEntry().add(ut);
        } else if (column == headerInfo.gethIndex()) {
            valueCheck(row, column, value);
            LiteralExpression le = new TLiteralExpression();
            le.setText(eValue(value));
            curRule.getOutputEntry().add(le);
        } else {
            LOG.trace("ignoring row {}, col {} having value {}", row, column, value);
        }
    }

    private String eValue(String value) {
        if (value.startsWith("“") &&value.endsWith("”")) {
            return "\""+value.substring(1, value.length()-1)+"\"";
        }
        return value;
    }

    private void valueCheck(int row, int column, String value) {
        if (value == null || value.isEmpty()) {
            throw new RuntimeException(headerInfo.toString() + row + " " + column);
        }
    }
    
}