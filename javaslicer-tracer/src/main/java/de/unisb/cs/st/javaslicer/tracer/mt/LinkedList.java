package de.unisb.cs.st.javaslicer.tracer.mt;



public class LinkedList<T> implements Cloneable {
	public static class Node<Z> implements Cloneable{
		public Z entry;
		public Node<Z> next;
	}
	public Object clone() throws CloneNotSupportedException
	{
		return super.clone();
	}
	private int size;
	private Node<T> first;
	private Node<T> last;
	public boolean addUnique(T o)
	{
		Node<T> i = first.next;
		while(i != null)
		{
			if(i.entry == o)
				return false;
			i = i.next;
		}
		Node<T> n = new Node<T>();
		n.entry = o;
		last.next=n;
		last = n;
		size++;
		return true;
	}
//	public synchronized boolean addAll(LinkedList<T> o)
//	{
//		boolean added = false;
//		Node<T> i = o.getFirst();
//		while(i != null)
//		{
//			if(i.entry != null)
//				added |= addUnique(i.entry);
////			Node<T> n = new Node<T>();
////			n.entry = i.entry;
////			last.next=n;
////			last = n;
//			i = i.next;
//		}
//		return added;
//	}
	public void add(T o)
	{
	    size++;
		Node<T> n = new Node<T>();
		n.entry = o;
		if(first.next == null)
		{
			first.next=n;
			last = n;
		}
		else
		{
			n.next = first.next;
			first.next = n;
		}
		
	}

	public Node<T> getFirst()
	{
		return first.next;
	}
	public LinkedList()
	{
		clear();
	}
	@Override
	public String toString() {
		StringBuffer ret = new StringBuffer();
		ret.append("[");
		Node<T> e = getFirst();
		while(e != null)
		{
			ret.append(e.entry);
			ret.append(",");
			e = e.next;
		}
		ret.append("]");
		return ret.toString();
	}
	public void clear() {
		first = new Node<T>();
		last = first;
	}
    public int getSize() {
        return size;
    }
}