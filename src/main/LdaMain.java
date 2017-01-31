package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import process.BuildArff;
import model.BugReportParser;
import model.Prediction;
import util.FileUtil;
import domain.Arff;
import domain.BugReport;
import model.LdaModel;

public class LdaMain {

	public static void main(String args[]) throws Exception {

		String project = "openstack";

		List<String> list = FileUtil.readLinesFromFile("data/" + project);
		List<BugReport> brs = new ArrayList<BugReport>();

		for (String str : list) {

			BugReport br = BugReportParser.parseBugReport(str);
			if (br.getAffectedPackage().isEmpty())
				continue;

			brs.add(br);
			if (brs.size() == 20000)
				break;
			// br.printInfo();
		}

		Collections.sort(brs);// sort bug reports according to ID
		System.out.println(brs.size() + " " + brs.get(0).getId() + " " + brs.get(brs.size() - 1).getId());

		Arff arff = BuildArff.buildArff("data/", project, brs);
		BuildArff.buildGroundTruth(arff);
		BuildArff.printStatistics(arff);

		double ratios[] = new double[10];
		Arrays.fill(ratios, 1.0 / ratios.length);
		List<Arff> folds = BuildArff.stratifyArff(arff, ratios);

		LdaModel lda = new LdaModel(100, 100);

		// lda.trainModel(arff.getBrs());

		int foldIndex = 1;
		Arff trainArff, testArff;
		trainArff = folds.get(0);

		double recall5, recall10;
		recall5 = recall10 = 0;

		while (foldIndex < folds.size()) {

			System.out.println("Now test fold " + foldIndex);
			testArff = folds.get(foldIndex);

			Prediction predict = new Prediction();

			predict.runLDA(lda, trainArff, testArff);

			recall5 += predict.evaluate(testArff, 5);
			recall10 += predict.evaluate(testArff, 10);

			// this fold test ended
			trainArff = BuildArff.combineArff(trainArff, testArff);
			foldIndex++;

			System.out.println("\n---------\n");
		}

		System.out.println("\nAverage Recall: " + recall5 / 9 + " " + recall10 / 9);

	}

}
