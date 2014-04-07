package edu.isi.karma.controller.command.worksheet;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.isi.karma.controller.command.Command;
import edu.isi.karma.controller.command.CommandException;
import edu.isi.karma.controller.command.CommandType;
import edu.isi.karma.controller.command.WorksheetCommand;
import edu.isi.karma.controller.update.ErrorUpdate;
import edu.isi.karma.controller.update.UpdateContainer;
import edu.isi.karma.controller.update.WorksheetListUpdate;
import edu.isi.karma.controller.update.WorksheetUpdateFactory;
import edu.isi.karma.rep.HNode;
import edu.isi.karma.rep.RepFactory;
import edu.isi.karma.rep.Row;
import edu.isi.karma.rep.Worksheet;
import edu.isi.karma.rep.Workspace;
import edu.isi.karma.rep.Node;
import edu.isi.karma.util.Util;

public class UnfoldCommand extends WorksheetCommand {

	private String hNodeId;
	//add column to this table
	private String hTableId;
	Command cmd;
	private Collection<HNode> hnodes = new ArrayList<HNode>();
	//the id of the new column that was created
	//needed for undo
	private String newHNodeId;

	private static Logger logger = LoggerFactory
			.getLogger(FoldCommand.class);

	public enum JsonKeys {
		updateType, hNodeId, worksheetId
	}
	
	protected UnfoldCommand(String id, String worksheetId, 
			String hTableId, String hNodeId) {
		super(id, worksheetId);
		this.hNodeId = hNodeId;
		this.hTableId = hTableId;
		
		addTag(CommandTag.Transformation);
	}

	@Override
	public String getCommandName() {
		// TODO Auto-generated method stub
		return "Unfold";
	}

	@Override
	public String getTitle() {
		// TODO Auto-generated method stub
		return "Unfold";
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "Unfold";
	}

	@Override
	public CommandType getCommandType() {
		// TODO Auto-generated method stub
		return CommandType.notUndoable;
	}

	@Override
	public UpdateContainer doIt(Workspace workspace) throws CommandException {
		RepFactory factory = workspace.getFactory();
		Worksheet oldws = workspace.getWorksheet(
				worksheetId);	
		Worksheet newws = factory.createWorksheet("Unfold: " + oldws.getTitle(), workspace, oldws.getEncoding());
		ArrayList<HNode> topHNodes = new ArrayList<HNode>(oldws.getHeaders().getHNodes());
		ArrayList<Row> rows = oldws.getDataTable().getRows(0, oldws.getDataTable().getNumRows());
		HNode key = topHNodes.get(0);
		HNode value = topHNodes.get(1);
		List<HNode> hnodes = new ArrayList<HNode>();
		for (HNode h : topHNodes) {
			if (h.getId().compareTo(value.getId()) != 0 && h.getId().compareTo(key.getId()) != 0)
				hnodes.add(h);
		}
		CloneTableUtils.cloneHTable(oldws.getHeaders(), newws.getHeaders(), newws, factory, hnodes);
		Map<String, String> keyMapping = new HashMap<String, String>();
		Map<String, String> HNodeidMapping = new HashMap<String, String>();
		for (Row row : rows) {
			Node n = row.getNode(key.getId());
			keyMapping.put(HashValueManager.getHashValue(oldws, n.getId()), n.getValue().asString());
		}
		for (String mapkey : keyMapping.keySet()) {
			HNode n = newws.getHeaders().addHNode(keyMapping.get(mapkey), newws, factory);
			HNodeidMapping.put(keyMapping.get(mapkey), n.getId());
		}
		
		for (Row row : rows) {
			Row newrow = CloneTableUtils.cloneDataTable(row, newws.getDataTable(), oldws.getHeaders(), newws.getHeaders(), hnodes, factory);
			String newId = HNodeidMapping.get(row.getNode(key.getId()).getValue().asString());
			Node newnode = newrow.getNode(newId);
			Node oldnode = row.getNode(value.getId());
			newnode.setValue(oldnode.getValue().asString(), oldnode.getStatus(), factory);
			
		}
		try{
			UpdateContainer c =  new UpdateContainer();
			c.add(new WorksheetListUpdate());
			c.append(WorksheetUpdateFactory.createRegenerateWorksheetUpdates(oldws.getId()));
			c.append(WorksheetUpdateFactory.createWorksheetHierarchicalAndCleaningResultsUpdates(newws.getId()));
			c.append(computeAlignmentAndSemanticTypesAndCreateUpdates(workspace));
			return c;
		} catch (Exception e) {
			logger.error("Error in UnfoldCommand" + e.toString());
			Util.logException(logger, e);
			return new UpdateContainer(new ErrorUpdate(e.getMessage()));
		}
	}

	@Override
	public UpdateContainer undoIt(Workspace workspace) {
		// TODO Auto-generated method stub
		return null;
	}

}
