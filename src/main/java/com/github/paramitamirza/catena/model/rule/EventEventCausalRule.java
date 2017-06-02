package com.github.paramitamirza.catena.model.rule;

import java.util.ArrayList;
import java.util.List;

import com.github.paramitamirza.catena.model.CandidateLinks;
import com.github.paramitamirza.catena.model.feature.CausalSignalList;
import com.github.paramitamirza.catena.model.feature.EventEventFeatureVector;
import com.github.paramitamirza.catena.model.feature.Marker;
import com.github.paramitamirza.catena.model.feature.PairFeatureVector;
import com.github.paramitamirza.catena.model.feature.TemporalSignalList;
import com.github.paramitamirza.catena.parser.entities.CausalRelation;
import com.github.paramitamirza.catena.parser.entities.Doc;
import com.github.paramitamirza.catena.parser.entities.Entity;
import com.github.paramitamirza.catena.parser.entities.EntityEnum;
import com.github.paramitamirza.catena.model.feature.*;

public class EventEventCausalRule {
	
	private String relType;
	
	public EventEventCausalRule(PairFeatureVector fv) {
		
	}
	
	public EventEventCausalRule(EventEventFeatureVector eefv) throws Exception {
		this.setRelType("O");
		String eventRule = getEventCausalityRule(eefv); 
		if (!eventRule.equals("O") && !eventRule.equals("NONE")) {
			if (eventRule.contains("-R")) this.setRelType("CLINK-R");
			else this.setRelType("CLINK");
		} else {
			this.setRelType("NONE");
		}
	}
	
	public static List<String> getEventEventClinksPerFile(Doc doc) throws Exception {
		List<String> ee = new ArrayList<String>();
		
		TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
	    
		List<CausalRelation> candidateClinks = doc.getCandidateClinks();	//candidate pairs
		
		for (CausalRelation clink : candidateClinks) {
			
			if (!clink.getSourceID().equals(clink.getTargetID())
					&& doc.getEntities().containsKey(clink.getSourceID())
					&& doc.getEntities().containsKey(clink.getTargetID())
					) {
				
				Entity e1 = doc.getEntities().get(clink.getSourceID());
				Entity e2 = doc.getEntities().get(clink.getTargetID());
				PairFeatureVector fv = new PairFeatureVector(doc, e1, e2, 
						CandidateLinks.getClinkType(e1.getID(), e2.getID(), doc), tsignalList, csignalList);
				
				if (fv.getPairType().equals(FeatureEnum.PairType.event_event)) {
					EventEventFeatureVector eefv = new EventEventFeatureVector(fv);
					EventEventCausalRule eeRule = new EventEventCausalRule(eefv);
					if (!eeRule.getRelType().equals("NONE")) {
						ee.add(eefv.getE1().getID() + "\t" + eefv.getE2().getID() + "\t" + 
								eefv.getLabel() + "\t" + eeRule.getRelType());
//					} else {
//						ee.add(eefv.getE1().getID() + "\t" + eefv.getE2().getID() + "\t" + 
//								eefv.getLabel() + "\tNONE");
					}
				}
			}
		}
		return ee;
	}
	
	public String getEventCausalityRule(EventEventFeatureVector eefv) throws Exception {
		String cVerb = "O", construction = "O";		
		if (eefv.getE1().getSentID().equals(eefv.getE2().getSentID())) {	//in the same sentence
			Marker m = eefv.getCausalVerb();
//			Marker mSig = eefv.getCausalSignal();
//			if (mSig.getText().equals("O") 
//					|| mSig.getText().equals("result")
//					|| mSig.getText().equals("by")) {
				if (!m.getCluster().equals("O")) {
					if (m.getCluster().equals("AFFECT")) {
						if ((m.getDepRelE1().equals("SBJ") 
								|| m.getDepRelE1().equals("ADV")
								|| m.getDepRelE1().equals("NMOD")
								|| m.getDepRelE1().equals("APPO")
								|| m.getDepRelE1().equals("PRD-IM")
								|| m.getDepRelE1().equals("PRP-IM"))) {
							if (m.getDepRelE2().equals("OBJ")) {
								cVerb = "AFFECT";
							} else {
								cVerb = "NONE";
							}
						} else {
							cVerb = "NONE";
						}
					} else if (m.getCluster().contains("LINK")) {
						if ((m.getDepRelE1().equals("SBJ") 
								|| m.getDepRelE1().equals("ADV")
								|| m.getDepRelE1().equals("NMOD")
								|| m.getDepRelE1().equals("APPO")
								|| m.getDepRelE1().equals("PRD-IM")
								|| m.getDepRelE1().equals("APPO-OPRD-IM")
								|| m.getDepRelE1().equals("NMOD-OPRD-IM")
								|| m.getDepRelE1().equals("PRP-IM"))) {
							if (m.getDepRelE2().equals("DIR-PMOD")
									|| m.getDepRelE2().equals("ADV-PMOD")
									|| m.getDepRelE2().equals("NMOD-PMOD")
									|| m.getDepRelE2().equals("AMOD-PMOD")) {
								cVerb = m.getCluster();
								
//							} else if ((m.getDepRelE2().equals("OBJ")
//									|| m.getDepRelE2().equals("OBJ-NMOD")
//									)
//									&& (m.getDepRelE1().equals("ADV")
//											|| m.getDepRelE1().equals("SBJ")
//											|| m.getDepRelE1().equals("APPO")
//											|| m.getDepRelE1().equals("OBJ-APPO")
//											|| m.getDepRelE1().equals("PRP-IM"))
//									&& (m.getText().equals("reflect")
//	//								|| m.getText().equals("follow")
//								)) {
//								cVerb = m.getCluster();
	//						} else if (m.getDepRelE2().equals("LGS-PMOD")
	//								&& m.getText().equals("follow")) {
	//							cVerb = "LINK";
							} else {
								cVerb = "NONE";
							}
						} else {
							cVerb = "NONE";
						}
					} else if (m.getCluster().equals("CAUSE")) {
						if ((m.getDepRelE1().equals("SBJ") 
								|| m.getDepRelE1().equals("ADV")
								|| m.getDepRelE1().equals("NMOD")
								|| m.getDepRelE1().equals("APPO")
								|| m.getDepRelE1().equals("PRD-IM")
								|| m.getDepRelE1().equals("PRP-IM"))) {
							if (m.getDepRelE2().equals("OBJ")) {
								cVerb = "CAUSE";
								construction = "BASIC";
							} else if (m.getDepRelE2().equals("OPRD-IM")
									|| m.getDepRelE2().equals("OPRD")) {
								cVerb = "CAUSE";
								construction = "PERIPHRASTIC";
							} else if (m.getDepRelE2().equals("LGS-PMOD")) {
								cVerb = "CAUSE-R";
								construction = "PASS";
							} else {
								cVerb = "NONE";
							}
						} else {
							cVerb = "NONE";
						}
					} else if (m.getCluster().equals("CAUSE-AMBIGUOUS")) {
						if ((m.getDepRelE1().equals("SBJ") 
								|| m.getDepRelE1().equals("ADV")
								|| m.getDepRelE1().equals("NMOD")
								|| m.getDepRelE1().equals("PRD-IM")
								|| m.getDepRelE1().equals("PRP-IM")
								|| m.getDepRelE1().equals("SBJ-PMOD")
								|| m.getDepRelE1().equals("SBJ-ADV-PMOD"))) {
							if (m.getDepRelE2().equals("OPRD-IM")) {
								cVerb = "CAUSE";
								construction = "PERIPHRASTIC";
							} else if (m.getText().equals("make")
									&& m.getDepRelE2().equals("OPRD-SUB-IM")) {
								cVerb = "CAUSE";
								construction = "PERIPHRASTIC";
							} else {
								cVerb = "NONE";
							}
						} else {
							cVerb = "NONE";
						}
					} else if (m.getCluster().equals("PREVENT")) {
						if ((m.getDepRelE1().equals("SBJ") 
								|| m.getDepRelE1().equals("ADV")
								|| m.getDepRelE1().equals("NMOD")
								|| m.getDepRelE1().equals("APPO")
								|| m.getDepRelE1().equals("PRD-IM")
								|| m.getDepRelE1().equals("PRP-IM")
								|| m.getDepRelE1().equals("OBJ-IM"))) {
							if (m.getDepRelE2().equals("OBJ")) {
								cVerb = "PREVENT";
								construction = "BASIC";
							} else if (m.getDepRelE2().equals("OPRD-IM")
									|| m.getDepRelE2().equals("OPRD")
									|| m.getDepRelE2().equals("ADV-PMOD")) {
								cVerb = "PREVENT";
								construction = "PERIPHRASTIC";
							} else if (m.getDepRelE2().equals("LGS-PMOD")) {
								cVerb = "PREVENT-R";
								construction = "PERIPHRASTIC";
							} else {
								cVerb = "NONE";
							}
						} else {
							cVerb = "NONE";
						}
					} else if (m.getCluster().equals("PREVENT-AMBIGUOUS")) {
						if ((m.getDepRelE1().equals("SBJ") 
								|| m.getDepRelE1().equals("ADV")
								|| m.getDepRelE1().equals("NMOD")
								|| m.getDepRelE1().equals("PRD-IM")
								|| m.getDepRelE1().equals("PRP-IM")
								|| m.getDepRelE1().equals("ADV-PMOD-IM"))) {
							if (m.getDepRelE2().equals("ADV-PMOD")
									|| m.getDepRelE2().equals("OPRD")) {
								cVerb = "PREVENT";
								construction = "PERIPHRASTIC";
							} else {
								cVerb = "NONE";
							}
						} else {
							cVerb = "NONE";
						}
					} else if (m.getCluster().equals("ENABLE")) {
						if ((m.getDepRelE1().equals("SBJ") 
								|| m.getDepRelE1().equals("ADV")
								|| m.getDepRelE1().equals("NMOD")
								|| m.getDepRelE1().equals("APPO")
								|| m.getDepRelE1().equals("PRD-IM")
								|| m.getDepRelE1().equals("PRP-IM")
								|| m.getDepRelE1().equals("OBJ-NMOD")
								|| m.getDepRelE1().equals("IM")
								|| m.getDepRelE1().equals("NMOD-IM"))) {
							if (m.getDepRelE2().equals("OBJ")
									&& (m.getText().equals("ensure")
									|| m.getText().equals("guarantee"))) {
								cVerb = "ENABLE";
								construction = "BASIC";
							} else if (m.getDepRelE2().equals("OPRD-IM")
									|| m.getDepRelE2().equals("OPRD")
									|| m.getDepRelE2().equals("OBJ-IM")) {
								cVerb = "ENABLE";
								construction = "PERIPHRASTIC";
							} else {
								cVerb = "NONE";
							}
						} else {
							cVerb = "NONE";
						}
					}
					
					String link = "O";
					if (!cVerb.equals("O") && !cVerb.equals("NONE")) {
						if (cVerb.contains("-R")) link = "CLINK-R";
						else link = "CLINK";
					}
//					System.err.println(eefv.getDoc().getFilename()+"\t"
//							+eefv.getLabel()+"\t"+link+"\t"
//							+cVerb+ "\t"+m.getCluster()+"\t"
//							+construction+"\t"
//							+eefv.getE1().getID()+"\t"+eefv.getE2().getID()+"\t"
//							+m.getDepRelE1()+"|"+m.getDepRelE2());
				}
//			}
		}
		return cVerb;
	}
	
	public String getRelType() {
		return relType;
	}

	public void setRelType(String relType) {
		this.relType = relType;
	}

}
