package com.github.paramitamirza.catena.parser.entities;

import com.github.paramitamirza.catena.ParserConfig;
import com.github.paramitamirza.catena.model.CandidateLinks;
import com.github.paramitamirza.catena.parser.TimeMLParser;
import com.github.paramitamirza.catena.parser.TimeMLToColumns;
import com.github.paramitamirza.catena.parser.naftotxp.MainVerb;
import edu.stanford.nlp.util.ArrayMap;
import ixa.kaflib.Dep;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.WF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class NafParser {

	private static final Logger logger = Logger.getLogger(NafParser.class.getName());

	public static enum Field {
		token, token_id, sent_id,
		pos, lemma, chunk, ner, mate_pos, mate_lemma,
		deps, main_verb,
		role1, role2, role3, is_arg_pred, has_semrole,
		connective, morpho,
		supersense, ss_ner,
		coref_event,
		tmx_id, tmx_type, tmx_value,
		ev_id, ev_class, tense_aspect_pol, tense, aspect, pol,
		tsignal, csignal, tlink, clink;
	}

	private EntityEnum.Language language;

	private Entity currTimex = null;
	private Entity currEvent = null;
	private Entity currTSignal = null;
	private Entity currCSignal = null;
	private Sentence currSentence = null;

	private static final List<String> supportedLanguages = Arrays.asList(new String[]{"en"});

	private Field[] fields = {Field.token, Field.token_id, Field.sent_id, Field.lemma,
			Field.ev_id, Field.ev_class, Field.tense_aspect_pol,
			Field.tmx_id, Field.tmx_type, Field.tmx_value,
			Field.tsignal, Field.csignal,
			Field.pos, Field.chunk,
			Field.mate_lemma, Field.mate_pos, Field.deps, Field.main_verb};



	public static Doc parseDocument(KAFDocument kaf) throws Exception {
		Doc doc = new Doc(EntityEnum.Language.EN, kaf.getPublic().publicId);

		if(!supportedLanguages.contains(kaf.getLang().equals("en"))){
			throw new Exception("Only the following languages are supported:" + supportedLanguages.toString());
		}
		doc.setLang(EntityEnum.Language.EN);
		setSentences(doc, kaf);

		return doc;
	}


	private static Doc setSentences(Doc doc, KAFDocument kaf){
		for(int i=1; i<kaf.getSentences().size()+1; i++){
			List<WF> kafSentence = kaf.getSentences().get(i);
			WF wfEnd = kafSentence.get(kafSentence.size() - 1);

			String id = Integer.toString(i);
			String start = Integer.toString(kafSentence.get(0).getOffset());
			String end = Integer.toString(wfEnd.getOffset()+wfEnd.getLength());

			Sentence sentence = new Sentence(id, start, end);

			doc.getSentenceArr().add(id);
			doc.getSentences().put(id, sentence);
		}
		return doc;
	}

	private Integer getIndex(Field field) {
		Integer idx = -1;
		ArrayList<Field> arr_fields = new ArrayList<Field>(Arrays.asList(this.fields));
		if (arr_fields.contains(field)) {
			idx = arr_fields.indexOf(field);
		}
		return idx;
	}

	private String getMainPosFromMorpho(String morpho) {
        if (!morpho.isEmpty()) {
            String[] morphs = morpho.split("\\+");
            if (morphs.length == 1) {
            	return morphs[0];
            } else {
            	return morphs[1];
            }
        } else {
            return "O";
        }
	}

	private String getMainPosFromPos(String pos) {
        if (pos.charAt(0) == 'V') return "v";
        else if (pos.charAt(0) == 'N') return "n";
        else if (pos.charAt(0) == 'A' && pos.charAt(1) == 'T') return "art";
        else if (pos.charAt(0) == 'D') return "det";
        else if (pos.charAt(0) == 'A' && pos.charAt(1) == 'J') return "adj";
        else if (pos.charAt(0) == 'A' && pos.charAt(1) == 'V') return "adv";
        else if (pos.charAt(0) == 'C' && pos.charAt(1) == 'J') return "conj";
        else if (pos.charAt(0) == 'C' && pos.charAt(1) == 'R' && pos.charAt(2) == 'D') return "crd";
        else if (pos.charAt(0) == 'O' && pos.charAt(1) == 'R' && pos.charAt(2) == 'D') return "ord";
        else if (pos.charAt(0) == 'P' && pos.charAt(1) == 'N') return "pron";
        else if (pos.charAt(0) == 'P' && pos.charAt(1) == 'R') return "prep";
        else if (pos.charAt(0) == 'T' && pos.charAt(1) == 'O') return "to";
        else if (pos.charAt(0) == 'P' && pos.charAt(1) == 'O' && pos.charAt(2) == 'S') return "pos";
        else if (pos.charAt(0) == 'P' && pos.charAt(1) == 'U') return "punc";
        else if (pos.charAt(0) == 'X') return "neg";
        return "O";
	}

	private Boolean isMainVerb(String mainVerb) {
		if (!mainVerb.isEmpty()) {
			if (mainVerb.equals("mainVb")) return true;
			else return false;
		} else {
			return false;
		}
	}

	private Map<String, String> parseDependency(String dep) {
		Map<String, String> dependencies = null;
        if (!dep.equals("O")) {
        	dependencies = new HashMap<String, String>();
            for (String d : dep.split("\\|\\|")) {
            	String[] dependent = d.split(":");
                String tokendep = dependent[0];
                String deprel = dependent[1];
                dependencies.put(tokendep, deprel);
            }
        }
        return dependencies;
	}

	private String[] parseTenseAspectPol(String tense_aspect_pol) {
		String[] arr = {"O", "O", "O"};
        if (!tense_aspect_pol.equals("O") && !tense_aspect_pol.equals("_")) {
        	arr = tense_aspect_pol.split("\\+");
        }
        return arr;
	}

	private String[] parseCoreference(String coref) {
		if (!coref.equals("O") && !coref.equals("_")) {
        	return coref.split(":");
        }
        return null;
	}

	public static void parseLine(Doc doc, KAFDocument kaf) {
		ArrayList<String> cols = new ArrayList<String>(Arrays.asList(s.split("\t")));

		if(cols.get(0).contains("DCT_")) {
			String tmx_id = cols.get(getIndex(Field.tmx_id));
			Timex dct = new Timex(tmx_id, "O", "O");
			String type = cols.get(getIndex(Field.tmx_type));
			if (type.contains("B-") || type.contains("I-")) type = type.substring(2);
			dct.setAttributes(type,
					cols.get(getIndex(Field.tmx_value)));
			dct.setDct(true);
			dct.setEmptyTag(false);
			dct.setIndex(doc.getEntIdx()); doc.setEntIdx(doc.getEntIdx() + 1);
			doc.getEntities().put(tmx_id, dct);
			doc.setDct(dct);

		} else if(cols.get(0).contains("ETX_")) {
			String tmx_id = cols.get(getIndex(Field.tmx_id));
			Timex etx = new Timex(tmx_id, "O", "O");
			String type = cols.get(getIndex(Field.tmx_type));
			if (type.contains("B-") || type.contains("I-")) type = type.substring(2);
			etx.setAttributes(type,
					cols.get(getIndex(Field.tmx_value)));
			etx.setDct(false);
			etx.setEmptyTag(true);
			etx.setIndex(doc.getEntIdx()); doc.setEntIdx(doc.getEntIdx() + 1);
			doc.getEntities().put(tmx_id, etx);

		} else if(!cols.get(0).isEmpty()) {


			doc.setTokIdx(doc.getTokIdx() + 1);
			doc.getTokenArr().add(tok_id);
			doc.getTokens().put(tok_id, tok);

			//sentence info
			String sent_id = cols.get(getIndex(Field.sent_id));
			if (currSentence == null) {
				currSentence = new Sentence(sent_id, tok_id, tok_id);
			} else if (currSentence != null && sent_id.equals(currSentence.getID())) {
				currSentence.setEndTokID(tok_id);
			} else if (currSentence != null && !sent_id.equals(currSentence.getID())) {
				currSentence.setIndex(doc.getSentIdx()); doc.setSentIdx(doc.getSentIdx() + 1);
				doc.getSentenceArr().add(currSentence.getID());
				doc.getSentences().put(currSentence.getID(), currSentence);
				currSentence = new Sentence(sent_id, tok_id, tok_id);
			}

			//entity info
			String tmx_id = cols.get(getIndex(Field.tmx_id));
			String ev_id = cols.get(getIndex(Field.ev_id));
			String tsig_id = null, csig_id = null;
			if (getIndex(Field.tsignal) != -1) {
				tsig_id = cols.get(getIndex(Field.tsignal));
			} else if (getIndex(Field.csignal) != -1) {
				csig_id = cols.get(getIndex(Field.csignal));
			}

			//Timex
			if (currTimex == null && !tmx_id.equals("O")) {
				tok.setTimexID(tmx_id);
				currTimex = new Timex(tmx_id, tok_id, tok_id);
				String type = cols.get(getIndex(Field.tmx_type));
				if (type.contains("B-") || type.contains("I-")) type = type.substring(2);
				((Timex)currTimex).setAttributes(type,
						cols.get(getIndex(Field.tmx_value)));
				((Timex)currTimex).setDct(false);
				((Timex)currTimex).setEmptyTag(false);
			} else if (currTimex != null && tmx_id.equals(currTimex.getID())) {
				tok.setTimexID(tmx_id);
				currTimex.setEndTokID(tok_id);
			} else if (currTimex != null && currTimex instanceof Timex &&
					!tmx_id.equals(currTimex.getID()) &&
					tmx_id.equals("O")) {
				currTimex.setIndex(doc.getEntIdx()); doc.setEntIdx(doc.getEntIdx() + 1);
				currTimex.setSentID(currSentence.getID());
				doc.getEntities().put(currTimex.getID(), currTimex);
				currSentence.getEntityArr().add(currTimex.getID());
				currTimex = null;
			} else if (currTimex != null && currTimex instanceof Timex &&
					!tmx_id.equals(currTimex.getID()) &&
					!tmx_id.equals("O")) {
				currTimex.setIndex(doc.getEntIdx()); doc.setEntIdx(doc.getEntIdx() + 1);
				currTimex.setSentID(currSentence.getID());
				doc.getEntities().put(currTimex.getID(), currTimex);
				currSentence.getEntityArr().add(currTimex.getID());

				tok.setTimexID(tmx_id);
				currTimex = new Timex(tmx_id, tok_id, tok_id);
				String type = cols.get(getIndex(Field.tmx_type));
				if (type.contains("B-") || type.contains("I-")) type = type.substring(2);
				((Timex)currTimex).setAttributes(type,
						cols.get(getIndex(Field.tmx_value)));
				((Timex)currTimex).setDct(false);
				((Timex)currTimex).setEmptyTag(false);
			}

			//coreference info
			String[] coref = null;
			if (getIndex(Field.coref_event) != -1) {
				coref = parseCoreference(cols.get(getIndex(Field.coref_event)));
			}

			//Event
			if (currEvent == null && !ev_id.equals("O") && tmx_id.equals("O")) {
				tok.setEventID(ev_id);
				currEvent = new Event(ev_id, tok_id, tok_id);
				((Event)currEvent).setAttributes(cols.get(getIndex(Field.ev_class)),
						tense, aspect, pol);
				if (coref != null) {
					for (String c : coref) {
						((Event)currEvent).getCorefList().add(c);
					}
				}
			} else if (currEvent != null && ev_id.equals(currEvent.getID())) {
				tok.setEventID(ev_id);
				currEvent.setEndTokID(tok_id);
			} else if (currEvent != null && currEvent instanceof Event &&
					!ev_id.equals(currEvent.getID()) &&
					ev_id.equals("O")) {
				currEvent.setIndex(doc.getEntIdx()); doc.setEntIdx(doc.getEntIdx() + 1);
				currEvent.setSentID(currSentence.getID());
				doc.getEntities().put(currEvent.getID(), currEvent);
				currSentence.getEntityArr().add(currEvent.getID());
				currEvent = null;
			} else if (currEvent != null && currEvent instanceof Event &&
					!ev_id.equals(currEvent.getID()) &&
					!ev_id.equals("O")) {
				currEvent.setIndex(doc.getEntIdx()); doc.setEntIdx(doc.getEntIdx() + 1);
				currEvent.setSentID(currSentence.getID());
				doc.getEntities().put(currEvent.getID(), currEvent);
				currSentence.getEntityArr().add(currEvent.getID());

				tok.setEventID(ev_id);
				currEvent = new Event(ev_id, tok_id, tok_id);
				((Event)currEvent).setAttributes(cols.get(getIndex(Field.ev_class)),
						tense, aspect, pol);
				if (coref != null) {
					for (String c : coref) {
						((Event)currEvent).getCorefList().add(c);
					}
				}
			}

			//Temporal signals
			if (tsig_id != null) {
				if (currTSignal == null && !tsig_id.equals("O")) {
					tok.settSignalID(tsig_id);
					currTSignal = new TemporalSignal(tsig_id, tok_id, tok_id);
				} else if (currTSignal != null && tsig_id.equals(currTSignal.getID())) {
					tok.settSignalID(tsig_id);
					currTSignal.setEndTokID(tok_id);
				} else if (currTSignal != null && currTSignal instanceof TemporalSignal &&
						!tsig_id.equals(currTSignal.getID()) &&
						tsig_id.equals("O")) {
					doc.getTemporalSignals().put(tsig_id, ((TemporalSignal)currTSignal));
					currTSignal = null;
				} else if (currTSignal != null && currTSignal instanceof TemporalSignal &&
						!tsig_id.equals(currTSignal.getID()) &&
						!tsig_id.equals("O")) {
					tok.settSignalID(tsig_id);
					currTSignal = new TemporalSignal(tsig_id, tok_id, tok_id);
				}
			}

			//Causal signals
			if (csig_id != null) {
				if (currCSignal == null && !csig_id.equals("O")) {
					tok.setcSignalID(csig_id);
					currCSignal = new CausalSignal(csig_id, tok_id, tok_id);
				} else if (currCSignal != null && csig_id.equals(currCSignal.getID())) {
					tok.settSignalID(csig_id);
					currCSignal.setEndTokID(tok_id);
				} else if (currCSignal != null && currCSignal instanceof CausalSignal &&
						!csig_id.equals(currCSignal.getID()) &&
						csig_id.equals("O")) {
					doc.getCausalSignals().put(csig_id, ((CausalSignal)currCSignal));
					currCSignal = null;
				} else if (currCSignal != null && currCSignal instanceof CausalSignal &&
						!csig_id.equals(currCSignal.getID()) &&
						!csig_id.equals("O")) {
					tok.setcSignalID(tsig_id);
					currCSignal = new CausalSignal(csig_id, tok_id, tok_id);
				}
			}

			if (!tmx_id.equals("O") || !ev_id.equals("O")) {

				String tlinks = null, clinks = null;
				if (getIndex(Field.tlink) != -1 && getIndex(Field.tlink) < cols.size()) {
					tlinks = cols.get(getIndex(Field.tlink));
				}
				if (getIndex(Field.clink) != -1 && getIndex(Field.clink) < cols.size()) {
					clinks = cols.get(getIndex(Field.clink));
				}

				//Temporal links
				if (tlinks != null) {
					if (!tlinks.equals("O") && !tlinks.equals("_NULL_")) {
						for (String t : tlinks.split("\\|\\|")) {
			            	String[] tlink_str = t.split(":");
			            	if (tlink_str.length == 3) {
			            		TemporalRelation tlink = new TemporalRelation(tlink_str[0], tlink_str[1]);
			            		tlink.setRelType(tlink_str[2]);

			            		// deprecated! put tlinks into candidate array instead of gold array
//			            		if (!doc.getTlinks().contains(tlink)) {
//			            			doc.getTlinks().add(tlink);
//			            		}
//			            		doc.getTlinkTypes().put(tlink_str[0]+","+tlink_str[1], tlink_str[2]);

			            		if (!doc.getCandidateTlinks().contains(tlink)) {
			            			doc.getCandidateTlinks().add(tlink);
			            		}
			            	}
			            }
					}
				}

				//Causal links
				if (clinks != null) {
					if (!clinks.equals("O") && !clinks.equals("_NULL_")) {
						for (String c : clinks.split("\\|\\|")) {
							String[] clink_str = c.split(":");
							if (clink_str.length >= 2) {
								CausalRelation clink = new CausalRelation(clink_str[0], clink_str[1]);
								if (!doc.getClinks().contains(clink)) {
									doc.getClinks().add(clink);
								}
							}
						}
					}
				}
			}

		}

	}

	private static List<Token> getTokens(Doc doc, KAFDocument kaf){

		List<String> mainVerbs = MainVerb.detect_main_verbs(kaf);


		List<Token> tokens = new ArrayList<>();
		kaf.getTerms().forEach(term -> {

		//token basic info : token_id, sent_id, token_string !!required!!
			String tok_id = term.getId();
			String sent_id = Integer.toString(term.getSent());
			String token_string = term.getStr();

		Token tok = new Token(tok_id, sent_id, token_string);

		//lemma, PoS and chunk
			tok.setLemma(term.getLemma());

			tok.setPos(term.getPos());
			tok.setMainPos(term.getPos());

//		if (getIndex(Field.chunk) != -1) {
//			tok.setChunk(cols.get(getIndex(Field.chunk)));
//		}

		//named entitiy type
			List<ixa.kaflib.Entity> entities = kaf.getEntitiesByTerm(term);
			if(!entities.isEmpty())
			tok.setNamedEntity(entities.get(0).getType());


		//WordNet supersense
		if (getIndex(Field.supersense) != -1) {
			tok.setWnSupersense(term.getExternalRefs().get(0));
		}

		//discourse connective
		if (getIndex(Field.connective) != -1) {
			tok.setDiscourseConn(cols.get(getIndex(Field.connective)));
		}

		//dependency relations
			List<Dep> deps = kaf.getDepsByTerm(term);
		Map<String, String> depRel = new HashMap<>();
			deps.forEach(dep -> {
				String to = dep.getTo().getStr();
				String from = dep.getFrom().getType();
			}
			tok.setDependencyRel(deps.get(0).getRfunc());


			//main verb
		if (getIndex(Field.main_verb) != -1) {
			tok.setMainVerb(term.);
		}

		String tense = "O", aspect = "O", pol = "O";
		//tense, aspect, polarity
		if (getIndex(Field.tense_aspect_pol) != -1) {
			String[] arr = parseTenseAspectPol(cols.get(getIndex(Field.tense_aspect_pol)));
			tense = arr[0];
			aspect = arr[1];
			pol = arr[2];
			tok.setTense(tense); tok.setAspect(aspect); tok.setPolarity(pol);
		} else if (getIndex(Field.tense) != -1){
			tense = cols.get(getIndex(Field.tense));
			tok.setTense(tense);
		} else if (getIndex(Field.aspect) != -1) {
			aspect = cols.get(getIndex(Field.aspect));
			tok.setAspect(aspect);
		} else if (getIndex(Field.pol) != -1) {
			kaf.getOpinionsBySent()
		}
			pol = cols.get(getIndex(Field.pol));
			tok.setPolarity(pol);
		}

		tok.setIndex(doc.getTokIdx());
	}












	public void printParseResult(Doc doc) {
		//array of tokens
		System.out.println("*** Token info - token per line ***");
		for (String tid : doc.getTokenArr()) {
			Token tok = doc.getTokens().get(tid);
			System.out.print(tok.getText() +
					"\t" + tok.getLemma() +
					"\t" + tok.getPos() +
					"\t" + tok.getMainPos() +
					"\t" + tok.getChunk() +
					"\t" + tok.getNamedEntity());
			System.out.print("\t");
			if (tok.getDependencyRel() != null) {
				for (String key : tok.getDependencyRel().keySet()) {
					System.out.print(key + "-" + tok.getDependencyRel().get(key) + " | ");
				}
				System.out.println();
			} else {
				System.out.println();
			}
		}

		System.out.println();

		//array of sentences
		System.out.println("*** Sentence info - sentence per line ***");
		for (String sid : doc.getSentenceArr()) {
			Sentence sent = doc.getSentences().get(sid);
			System.out.print(sent.getID() +
					"\t" + sent.getStartTokID() +
					"\t" + sent.getEndTokID());
			System.out.print("\t");
			for (String eid : sent.getEntityArr()) {
				System.out.print(eid + " | ");
			}
			System.out.println();
		}

		System.out.println();

		//array of entities
		System.out.println("*** Entities (Events and Timexes) - entity per line ***");
		for (String ent_id : doc.getEntities().keySet()) {
			Entity ent = doc.getEntities().get(ent_id);
			if (ent instanceof Timex) {
				System.out.println(ent.getID() + "\tTimex\t" + ent.getStartTokID() +
						"\t" + ent.getEndTokID());
			} else if (ent instanceof Event) {
				System.out.print(ent.getID() + "\tEvent\t" + ent.getStartTokID() +
						"\t" + ent.getEndTokID());
				System.out.print("\t");
				for (String eid : ((Event)ent).getCorefList()) {
					System.out.print(eid + "|");
				}
				System.out.println();
			}
		}

		System.out.println();

		//array of TLINKs
		System.out.println("*** TLINKs - TLINK per line ***");
		for (TemporalRelation tlink : doc.getTlinks()) {
			System.out.println(tlink.getSourceID() + "\t" + tlink.getTargetID() +
					"\t" + tlink.getRelType());
		}

		//array of candidate-TLINKs
		System.out.println("*** Candidate-TLINKs - TLINK per line ***");
		for (TemporalRelation tlink : doc.getCandidateTlinks()) {
			System.out.println(tlink.getSourceID() + "\t" + tlink.getTargetID() +
					"\t" + tlink.getRelType());
		}
	}



}
