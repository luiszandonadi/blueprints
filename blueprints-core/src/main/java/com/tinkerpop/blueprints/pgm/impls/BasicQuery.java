package com.tinkerpop.blueprints.pgm.impls;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Query;
import com.tinkerpop.blueprints.pgm.Vertex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class BasicQuery implements Query {

    private final Vertex vertex;
    public Direction direction = Direction.BOTH;
    public String[] labels;
    public long limit = Long.MAX_VALUE;
    public List<HasContainer> hasContainers = new LinkedList<HasContainer>();

    public BasicQuery(final Vertex vertex) {
        this.vertex = vertex;
    }

    public Query has(final String key, final Object value) {
        this.hasContainers.add(new HasContainer(key, value, Compare.EQUAL));
        return this;
    }

    public Query has(final String key, final Object value, final Compare compare) {
        this.hasContainers.add(new HasContainer(key, value, compare));
        return this;
    }

    public Query interval(final String key, final Object startValue, final Object endValue) {
        this.hasContainers.add(new HasContainer(key, startValue, Compare.GREATER_THAN_EQUAL));
        this.hasContainers.add(new HasContainer(key, endValue, Compare.LESS_THAN));
        return this;
    }

    public Query direction(final Direction direction) {
        this.direction = direction;
        return this;
    }

    public Query labels(final String... labels) {
        this.labels = labels;
        return this;
    }

    public Query limit(final long max) {
        this.limit = max;
        return this;
    }

    public Iterable<Edge> edges() {
        return new QueryIterable<Edge>(false);
    }

    public Iterable<Vertex> vertices() {
        return new QueryIterable<Vertex>(true);
    }

    public long count() {
        long count = 0;
        for (final Edge edge : this.edges()) {
            count++;
        }
        return count;
    }

    public Object vertexIds() {
        final List<Object> list = new ArrayList<Object>();
        for (final Vertex vertex : this.vertices()) {
            list.add(vertex.getId());
        }
        return list;
    }

    private class HasContainer {
        public String key;
        public Object value;
        public Compare compare;

        public HasContainer(final String key, final Object value, final Compare compare) {
            this.key = key;
            this.value = value;
            this.compare = compare;
        }

        public boolean isLegal(final Element element) {
            final Object elementValue = element.getProperty(key);

            switch (compare) {
                case EQUAL:
                    if (null == elementValue)
                        return value == null;
                    return elementValue.equals(value);
                case NOT_EQUAL:
                    if (null == elementValue)
                        return value != null;
                    return !elementValue.equals(value);
                case GREATER_THAN:
                    if (null == elementValue || value == null)
                        return false;
                    return ((Comparable) elementValue).compareTo(value) >= 1;
                case LESS_THAN:
                    if (null == elementValue || value == null)
                        return false;
                    return ((Comparable) elementValue).compareTo(value) <= -1;
                case GREATER_THAN_EQUAL:
                    if (null == elementValue || value == null)
                        return false;
                    return ((Comparable) elementValue).compareTo(value) >= 0;
                case LESS_THAN_EQUAL:
                    if (null == elementValue || value == null)
                        return false;
                    return ((Comparable) elementValue).compareTo(value) <= 0;
                default:
                    throw new IllegalArgumentException("Invalid state as no valid filter was provided");
            }
        }
    }

    private class QueryIterable<T extends Element> implements Iterable<T> {

        private Iterable<Edge> iterable;
        private boolean isVertex;

        public QueryIterable(boolean isVertex) {
            this.isVertex = isVertex;
            List<Iterable<Edge>> temp = new ArrayList<Iterable<Edge>>(2);
            if (direction == Direction.OUT || direction == Direction.BOTH) {
                temp.add(vertex.getOutEdges(labels));
            }

            if (direction == Direction.IN || direction == Direction.BOTH) {
                temp.add(vertex.getInEdges(labels));
            }
            this.iterable = new MultiIterable<Edge>(temp);

        }

        public Iterator<T> iterator() {
            return new QueryIterator<T>();
        }

        private class QueryIterator<T extends Element> implements Iterator<T> {

            Edge nextEdge = null;
            Iterator<Edge> itty;
            long count = 0;

            public QueryIterator() {
                this.itty = iterable.iterator();
                this.loadNext();
            }

            public boolean hasNext() {
                return null != nextEdge;
            }

            public T next() {
                if (nextEdge != null) {
                    final Edge temp = nextEdge;
                    this.loadNext();
                    if (isVertex) {
                        if (direction == Direction.OUT)
                            return (T) temp.getInVertex();
                        else if (direction == Direction.IN)
                            return (T) temp.getOutVertex();
                        else {
                            if (temp.getInVertex() == vertex) {
                                return (T) temp.getOutVertex();
                            } else {
                                return (T) temp.getInVertex();
                            }
                        }

                    } else {
                        return (T) temp;
                    }
                } else
                    throw new NoSuchElementException();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            private void loadNext() {
                nextEdge = null;
                if (count >= limit) return;
                while (itty.hasNext() && nextEdge == null) {
                    Edge edge = itty.next();
                    boolean filter = false;
                    for (final HasContainer hasContainer : hasContainers) {
                        if (!hasContainer.isLegal(edge)) {
                            filter = true;
                            break;
                        }
                    }
                    if (!filter) {
                        this.nextEdge = edge;
                        this.count++;
                    }
                }
            }


        }
    }
}
