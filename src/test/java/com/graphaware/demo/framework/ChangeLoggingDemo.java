/*
 * Copyright (c) 2013 GraphAware
 *
 * This file is part of GraphAware.
 *
 * GraphAware is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.graphaware.demo.framework;

import com.graphaware.propertycontainer.dto.string.property.SerializablePropertiesImpl;
import com.graphaware.tx.event.improved.api.Change;
import com.graphaware.tx.event.improved.api.ImprovedTransactionData;
import com.graphaware.tx.event.improved.api.LazyTransactionData;
import com.graphaware.tx.executor.single.SimpleTransactionExecutor;
import com.graphaware.tx.executor.single.VoidReturningCallback;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.test.TestGraphDatabaseFactory;

import static org.neo4j.graphdb.Direction.OUTGOING;
import static org.neo4j.graphdb.DynamicRelationshipType.withName;

/**
 * An example of using the framework for logging every change (to std out).
 */
public class ChangeLoggingDemo {

    @Test
    public void demonstrateLoggingEveryChange() {
        GraphDatabaseService database = new TestGraphDatabaseFactory().newImpermanentDatabase();

        database.registerTransactionEventHandler(new ChangeLogger());

        SimpleTransactionExecutor executor = new SimpleTransactionExecutor(database);

        //create nodes
        executor.executeInTransaction(new VoidReturningCallback() {
            @Override
            protected void doInTx(GraphDatabaseService database) {
                Node node1 = database.createNode();
                node1.setProperty("name", "One");

                Node node2 = database.createNode();
                node2.setProperty("name", "Two");

                Node node3 = database.createNode();
                node3.setProperty("name", "Three");
            }
        });

        //create relationship
        executor.executeInTransaction(new VoidReturningCallback() {
            @Override
            protected void doInTx(GraphDatabaseService database) {
                Node one = database.getNodeById(1);
                Node two = database.getNodeById(2);

                Relationship relationship = one.createRelationshipTo(two, withName("TEST"));
                relationship.setProperty("level", 2);

                two.createRelationshipTo(one, withName("TEST"));
            }
        });

        //change and delete nodes
        executor.executeInTransaction(new VoidReturningCallback() {
            @Override
            protected void doInTx(GraphDatabaseService database) {
                database.getNodeById(3).delete();

                database.getNodeById(1).setProperty("name", "New One");
            }
        });

        //change and delete relationships
        executor.executeInTransaction(new VoidReturningCallback() {
            @Override
            protected void doInTx(GraphDatabaseService database) {
                database.getNodeById(2).getSingleRelationship(withName("TEST"), OUTGOING).delete();

                database.getNodeById(1).getSingleRelationship(withName("TEST"), OUTGOING).setProperty("level", 3);
            }
        });

    }

    private class ChangeLogger extends TransactionEventHandler.Adapter<Void> {

        @Override
        public Void beforeCommit(TransactionData data) throws Exception {
            ImprovedTransactionData improvedData = new LazyTransactionData(data);

            for (Node createdNode : improvedData.getAllCreatedNodes()) {
                System.out.println("Created node " + createdNode.getId()
                        + " with properties: " + new SerializablePropertiesImpl(createdNode).toString());
            }

            for (Node deletedNode : improvedData.getAllDeletedNodes()) {
                System.out.println("Deleted node " + deletedNode.getId()
                        + " with properties: " + new SerializablePropertiesImpl(deletedNode).toString());
            }

            for (Change<Node> changedNode : improvedData.getAllChangedNodes()) {
                System.out.println("Changed node " + changedNode.getCurrent().getId()
                        + " from properties: " + new SerializablePropertiesImpl(changedNode.getPrevious()).toString()
                        + " to properties: " + new SerializablePropertiesImpl(changedNode.getCurrent()).toString());
            }

            for (Relationship createdRelationship : improvedData.getAllCreatedRelationships()) {
                System.out.println("Created relationship " + createdRelationship.getId()
                        + " between nodes " + createdRelationship.getStartNode().getId()
                        + " and " + createdRelationship.getEndNode().getId()
                        + " with properties: " + new SerializablePropertiesImpl(createdRelationship).toString());
            }

            for (Relationship deletedRelationship : improvedData.getAllDeletedRelationships()) {
                System.out.println("Deleted relationship " + deletedRelationship.getId()
                        + " between nodes " + deletedRelationship.getStartNode().getId()
                        + " and " + deletedRelationship.getEndNode().getId()
                        + " with properties: " + new SerializablePropertiesImpl(deletedRelationship).toString());
            }

            for (Change<Relationship> changedRelationship : improvedData.getAllChangedRelationships()) {
                System.out.println("Changed relationship " + changedRelationship.getCurrent().getId()
                        + " between nodes " + changedRelationship.getCurrent().getStartNode().getId()
                        + " and " + changedRelationship.getCurrent().getEndNode().getId()
                        + " from properties: " + new SerializablePropertiesImpl(changedRelationship.getPrevious()).toString()
                        + " to properties: " + new SerializablePropertiesImpl(changedRelationship.getCurrent()).toString());
            }

            return null;
        }
    }
}