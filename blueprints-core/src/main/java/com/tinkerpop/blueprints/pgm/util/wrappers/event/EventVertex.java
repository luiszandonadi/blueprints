package com.tinkerpop.blueprints.pgm.util.wrappers.event;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Query;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.impls.BasicQuery;
import com.tinkerpop.blueprints.pgm.util.wrappers.event.listener.GraphChangedListener;
import com.tinkerpop.blueprints.pgm.util.wrappers.event.util.EventEdgeSequence;

import java.util.List;

/**
 * An vertex with a GraphChangedListener attached.  Those listeners are notified when changes occur to
 * the properties of the vertex.
 *
 * @author Stephen Mallette
 */
public class EventVertex extends EventElement implements Vertex {
    public EventVertex(final Vertex rawVertex, final List<GraphChangedListener> graphChangedListeners) {
        super(rawVertex, graphChangedListeners);
    }

    public Iterable<Edge> getInEdges(final String... labels) {
        return new EventEdgeSequence(((Vertex) this.rawElement).getInEdges(labels).iterator(), this.graphChangedListeners);
    }

    public Iterable<Edge> getOutEdges(final String... labels) {
        return new EventEdgeSequence(((Vertex) this.rawElement).getOutEdges(labels).iterator(), this.graphChangedListeners);
    }

    public Query query() {
        return new BasicQuery(this);
    }

    public Vertex getRawVertex() {
        return (Vertex) this.rawElement;
    }
}
