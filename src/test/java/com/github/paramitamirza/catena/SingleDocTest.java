package com.github.paramitamirza.catena;

import com.github.paramitamirza.catena.model.classifier.EventDctTemporalClassifier;
import com.github.paramitamirza.catena.model.classifier.EventEventTemporalClassifier;
import com.github.paramitamirza.catena.model.classifier.EventTimexTemporalClassifier;
import com.github.paramitamirza.catena.model.feature.PairFeatureVector;
import com.github.paramitamirza.catena.parser.ColumnParser;
import com.github.paramitamirza.catena.parser.TimeMLParser;
import com.github.paramitamirza.catena.parser.entities.Doc;
import com.github.paramitamirza.catena.parser.entities.EntityEnum;
import com.github.paramitamirza.catena.parser.entities.TLINK;
import eu.fbk.newsreader.naf.NAFtoTXP;
import ixa.kaflib.KAFDocument;
import org.jdom2.JDOMException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by sander.puts on 6/11/2017.
 */


@RunWith(SpringRunner.class)
public class SingleDocTest {

    private static final Logger logger = Logger.getLogger(SingleDocTest.class.getName());

    //TODO use property file
    private static final String dataPath = ".\\data\\";
    private static final String modelPath = ".\\models\\";

    private static String tlinkDensePath = dataPath + "TimebankDense.T3.txt";
    private static String clinkEval3Path = dataPath + "Causal-TempEval3-eval.txt";

    private static String tempEval3EvalPath = dataPath + "TempEval3-eval_TML/";
    private static String tempEval3TrainPath = dataPath + "TempEval3-train_TML/";

    private static String causalTimeBankPath = dataPath + "Causal-TimeBank_TML/";

    @Test
    public void tagDocumentCausal() throws IOException {

        String taskName = "te3";
        // ---------- TEMPORAL ---------- //

        String[] te3CLabel = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS",
                "INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
        String[] te3CLabelCollapsed = {"BEFORE", "AFTER", "IDENTITY", "SIMULTANEOUS",
                "INCLUDES", "IS_INCLUDED", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};

        Temporal temp = new Temporal(false, te3CLabelCollapsed,
                modelPath+ taskName + "-event-dct.model",
                modelPath + taskName + "-event-timex.model",
                modelPath + taskName + "-event-event.model",
                true, true, true,
                true, false);

        // Parse a document in column format (resulting from NewsReader text processing)
        try {
            ColumnParser.Field[] fields = {ColumnParser.Field.token, ColumnParser.Field.token_id, ColumnParser.Field.sent_id, ColumnParser.Field.pos,
                    ColumnParser.Field.lemma, ColumnParser.Field.deps, ColumnParser.Field.tmx_id, ColumnParser.Field.tmx_type, ColumnParser.Field.tmx_value,
                    ColumnParser.Field.ner, ColumnParser.Field.ev_class, ColumnParser.Field.ev_id, ColumnParser.Field.role1, ColumnParser.Field.role2,
                    ColumnParser.Field.role3, ColumnParser.Field.is_arg_pred, ColumnParser.Field.has_semrole, ColumnParser.Field.chunk,
                    ColumnParser.Field.main_verb, ColumnParser.Field.connective, ColumnParser.Field.morpho,
                    ColumnParser.Field.tense_aspect_pol, ColumnParser.Field.tlink};
            ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN, fields);

            Doc doc = colParser.parseDocument(new File("./data/example_column/wsj_1014.tml.txp"), true);

            //TimeML instances and links (parsed direcly from TimeML format)
            //TimeMLParser.parseTimeML(new File("./data/example_TML/wsj_1014.tml"), doc);


            String tmlFile = "./data/TempEval3-eval_TML/";

            // PREDICT
            Map<String, String> relTypeMapping = new HashMap<String, String>();
            relTypeMapping.put("IDENTITY", "SIMULTANEOUS");

            boolean goldCandidate = false;
            String[] labels = te3CLabel;
            logger.info("tlinks before: " + doc.getTlinks().size());

            // Init the classifier...
            List<TLINK> result = temp.extractRelationsDoc(taskName, doc, labels, relTypeMapping);
            logger.info("tlinks after: " + doc.getTlinks().size());
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getStackTrace().toString());
        }



    }

    @Test
    @Ignore //perl???
    public void kafConversion() throws IOException, InterruptedException, ParserConfigurationException, SAXException, JDOMException, JAXBException, URISyntaxException {
        File file = new File(SingleDocTest.this.getClass().getResource("/wikinews_1173_en.naf").getFile());
        KAFDocument nafFile = KAFDocument.createFromFile(file);
        boolean writePos = true;
        String[] listCol = new String[]{};
        String nameFile = "./outputtest.txp";
        String typeCorpus = "timex";

        logger.info(nafFile.toString());

        NAFtoTXP.NAF2TXP(nafFile, writePos, listCol, nameFile, typeCorpus);
    }


}
