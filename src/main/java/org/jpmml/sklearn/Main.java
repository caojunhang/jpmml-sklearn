/*
 * Copyright (c) 2015 Villu Ruusmann
 *
 * This file is part of JPMML-SkLearn
 *
 * JPMML-SkLearn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-SkLearn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-SkLearn.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpmml.sklearn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.dmg.pmml.Extension;
import org.dmg.pmml.MiningBuildTask;
import org.dmg.pmml.PMML;
import org.jpmml.converter.Schema;
import org.jpmml.model.MetroJAXBUtil;
import sklearn.Estimator;
import sklearn_pandas.DataFrameMapper;

public class Main {

	@Parameter (
		names = {"--pkl-input", "--pkl-estimator-input"},
		description = "Estimator pickle input file",
		required = true
	)
	private File estimatorInput = null;

	@Parameter (
		names = {"--repr-estimator"},
		description = "Estimator string representation",
		hidden = true
	)
	private String estimatorRepr = null;

	@Parameter (
		names = "--help",
		description = "Show the list of configuration options and exit",
		help = true
	)
	private boolean help = false;

	@Parameter (
		names = "--pkl-mapper-input",
		description = "DataFrameMapper pickle input file",
		required = false
	)
	private File mapperInput = null;

	@Parameter (
		names = "--repr-mapper",
		description = "DataFrameMapper string representation",
		hidden = true
	)
	private String mapperRepr = null;

	@Parameter (
		names = "--pmml-output",
		description = "PMML output file",
		required = true
	)
	private File output = null;


	static
	public void main(String... args) throws Exception {
		Main main = new Main();

		JCommander commander = new JCommander(main);
		commander.setProgramName(Main.class.getName());

		try {
			commander.parse(args);
		} catch(ParameterException pe){
			commander.usage();

			System.exit(-1);
		}

		if(main.help){
			commander.usage();

			System.exit(0);
		}

		main.run();
	}

	private void run() throws Exception {
		PMML pmml;

		Schema schema;

		try(Storage storage = PickleUtil.createStorage(this.estimatorInput)){
			Object object = PickleUtil.unpickle(storage);

			if(!(object instanceof Estimator)){
				throw new IllegalArgumentException("The estimator object (" + ClassDictUtil.formatClass(object) + ") is not an Estimator or is not a supported Estimator subclass");
			}

			Estimator estimator = (Estimator)object;

			schema = estimator.createSchema();

			pmml = estimator.encodePMML(schema);
		}

		if(this.estimatorRepr != null){
			addObjectRepr(pmml, "estimator", this.estimatorRepr);
		} // End if

		if(this.mapperInput != null){

			try(Storage storage = PickleUtil.createStorage(this.mapperInput)){
				Object object = PickleUtil.unpickle(storage);

				if(!(object instanceof DataFrameMapper)){
					throw new IllegalArgumentException("The mapper object (" + ClassDictUtil.formatClass(object) + ") is not a DataFrameMapper");
				}

				DataFrameMapper mapper = (DataFrameMapper)object;

				mapper.updatePMML(schema, pmml);
			}

			if(this.mapperRepr != null){
				addObjectRepr(pmml, "mapper", this.mapperRepr);
			}
		}

		try(OutputStream os = new FileOutputStream(this.output)){
			MetroJAXBUtil.marshalPMML(pmml, os);
		}
	}

	public File getEstimatorInput(){
		return this.estimatorInput;
	}

	public void setEstimatorInput(File estimatorInput){
		this.estimatorInput = estimatorInput;
	}

	public File getMapperInput(){
		return this.mapperInput;
	}

	public void setMapperInput(File mapperInput){
		this.mapperInput = mapperInput;
	}

	public File getOutput(){
		return this.output;
	}

	public void setOutput(File output){
		this.output = output;
	}

	static
	private void addObjectRepr(PMML pmml, String name, String content){
		MiningBuildTask miningBuildTask = pmml.getMiningBuildTask();

		if(miningBuildTask == null){
			miningBuildTask = new MiningBuildTask();

			pmml.setMiningBuildTask(miningBuildTask);
		}

		Extension extension = new Extension()
			.setName(name)
			.setValue("repr(" + name + ")")
			.addContent(content);

		miningBuildTask.addExtensions(extension);
	}
}