package fitnesse.responders.run.formatters;

import fitnesse.FitNesseContext;
import fitnesse.FitNesseVersion;
import fitnesse.responders.run.TestSummary;
import static fitnesse.responders.run.SuiteExecutionReport.PageHistoryReference;
import fitnesse.testutil.FitNesseUtil;
import fitnesse.wiki.InMemoryPage;
import fitnesse.wiki.WikiPage;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import util.DateTimeUtil;
import util.TimeMeasurement;
import util.XmlUtil;

import java.io.StringWriter;
import java.util.Date;
import java.util.List;

public class SuiteHistoryFormatterTest {
  private SuiteHistoryFormatter formatter;
  private WikiPage root;
  private FitNesseContext context;
  private WikiPage testPage;
  private StringWriter writer;
  private long testTime;
  private WikiPage suitePage;

  @Before
  public void setup() throws Exception {
    root = InMemoryPage.makeRoot("RooT");
    context = FitNesseUtil.makeTestContext(root);
    suitePage = root.addChildPage("SuitePage");
    testPage = suitePage.addChildPage("TestPage");
    writer = new StringWriter();
    formatter = new SuiteHistoryFormatter(context, suitePage, writer);
    testTime = DateTimeUtil.getTimeFromString("12/5/1952 1:19:00");
  }


  @Test
  public void shouldRememberTestSummariesInReferences() throws Exception {
    addTest();
    List<PageHistoryReference> references = formatter.getPageHistoryReferences();
    assertEquals(1, references.size());
    assertEquals(new TestSummary(1, 2, 3, 4), references.get(0).getTestSummary());
  }

  private void addTest() throws Exception {
    TimeMeasurement timeMeasurement = new TimeMeasurement() {
      @Override
      public long startedAt() {
        return testTime;
      }
      @Override
      public long stoppedAt() {
        return testTime+1;
      }
    };
    formatter.newTestStarted(testPage, timeMeasurement);
    formatter.testComplete(testPage, new TestSummary(1, 2, 3, 4), timeMeasurement);
  }

  @Test
  public void allTestingCompleteShouldProduceLinks() throws Exception {
    addTest();
    formatter.allTestingComplete();
    String output = writer.toString();
    Document document = XmlUtil.newDocument(output);
    Element suiteResultsElement = document.getDocumentElement();
    assertEquals("suiteResults", suiteResultsElement.getNodeName());
    assertEquals(new FitNesseVersion().toString(), XmlUtil.getTextValue(suiteResultsElement, "FitNesseVersion"));
    assertEquals("SuitePage", XmlUtil.getTextValue(suiteResultsElement, "rootPath"));

    NodeList xmlPageReferences = suiteResultsElement.getElementsByTagName("pageHistoryReference");
    assertEquals(1, xmlPageReferences.getLength());
    for (int referenceIndex = 0; referenceIndex < xmlPageReferences.getLength(); referenceIndex++) {
      Element pageHistoryReferenceElement = (Element) xmlPageReferences.item(referenceIndex);
      assertEquals("SuitePage.TestPage", XmlUtil.getTextValue(pageHistoryReferenceElement, "name"));
      assertEquals(DateTimeUtil.formatDate(new Date(testTime)), XmlUtil.getTextValue(pageHistoryReferenceElement, "date"));
      String link = "SuitePage.TestPage?pageHistory&resultDate=19521205011900";
      assertEquals(link, XmlUtil.getTextValue(pageHistoryReferenceElement, "pageHistoryLink"));
      Element countsElement = XmlUtil.getElementByTagName(pageHistoryReferenceElement, "counts");
      assertEquals("1", XmlUtil.getTextValue(countsElement, "right"));
      assertEquals("2", XmlUtil.getTextValue(countsElement, "wrong"));
      assertEquals("3", XmlUtil.getTextValue(countsElement, "ignores"));
      assertEquals("4", XmlUtil.getTextValue(countsElement, "exceptions"));
    }

    Element finalCounts = XmlUtil.getElementByTagName(suiteResultsElement, "finalCounts");
    assertEquals("0", XmlUtil.getTextValue(finalCounts, "right"));
    assertEquals("1", XmlUtil.getTextValue(finalCounts, "wrong"));
    assertEquals("0", XmlUtil.getTextValue(finalCounts, "ignores"));
    assertEquals("0", XmlUtil.getTextValue(finalCounts, "exceptions"));
  }
}
