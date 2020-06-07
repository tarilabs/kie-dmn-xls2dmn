package org.kie.dmn.xls2dmn;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.Scanner;

import org.junit.Test;

public class AppTest {

    @Test
    public void shouldAnswerWithTrue() {
        new ExcelParser().parseFile(this.getClass().getResourceAsStream("/Loan_approvals.xlsx"));
    }
}
