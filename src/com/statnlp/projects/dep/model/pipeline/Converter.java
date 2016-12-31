package com.statnlp.projects.dep.model.pipeline;

import java.util.ArrayList;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.projects.dep.DependInstance;
import com.statnlp.projects.dep.Transformer;
import com.statnlp.projects.entity.semi.SemiCRFInstance;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.UnnamedDependency;

public class Converter {

	/**
	 * Conver the semiCRFInstace to a dependency instance.
	 * @param insts: semiCRFInstance output from semi-CRFs parser.
	 * @return
	 */
	public static DependInstance[] semiInst2DepInst(Instance[] insts) {
		DependInstance[] depInsts = new DependInstance[insts.length];
		for (int idx = 0; idx < insts.length; idx++) {
			SemiCRFInstance inst = (SemiCRFInstance)insts[idx];
			Sentence sent = inst.getInput();
			WordToken[] newWTs = new WordToken[sent.length() + 1]; //because semi-CRFs is 0-indexed.
			newWTs[0] = new WordToken("ROOT", "ROOT", -1, "O");
			String[] semiPredEntities = inst.toEntities(inst.getPrediction());
			for (int w = 0; w < sent.length(); w++) {
				newWTs[w + 1] = new WordToken(sent.get(w).getName(), sent.get(w).getTag(), sent.get(w).getHeadIndex()+1, semiPredEntities[w]);
			}
			Sentence newSent = new Sentence(newWTs);
			DependInstance depInst = new DependInstance(inst.getInstanceId(), 1.0, newSent);
			depInst.setUnlabeled();
			depInsts[idx] = depInst;
		}
		return depInsts;
	}
	
	public static SemiCRFInstance[] depInst2SemiInst(Instance[] insts) {
		SemiCRFInstance[] semiInsts = new SemiCRFInstance[insts.length];
		for (int idx = 0; idx < insts.length; idx ++) {
			DependInstance depInst = (DependInstance)insts[idx];
			Sentence depSent = depInst.getInput();
			Tree prediction = depInst.getPrediction();
			ArrayList<UnnamedDependency> predDependencies = depInst.toDependencies(prediction);
			int[] predHeads = Transformer.getHeads(predDependencies, depSent);
			WordToken[] wts = new WordToken[depSent.length()-1];
			for (int w = 0; w < wts.length; w++) {
				wts[w] = new WordToken(depSent.get(w+1).getName(), depSent.get(w+1).getTag(), predHeads[w+1]-1, depSent.get(w+1).getEntity());
			}
			Sentence semiSent = new Sentence(wts);
			SemiCRFInstance semiInst = new SemiCRFInstance(depInst.getInstanceId(), semiSent, null);
			semiInsts[idx] = semiInst;
		}
		return semiInsts;
	}

}
