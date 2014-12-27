package com.kurt.swapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class ExpandableListAdapter extends BaseExpandableListAdapter {
    
    private Context context;
    private ArrayList<String> listDataHeader;
    private HashMap<String, ArrayList<String>> listDataChild;
    
    public ExpandableListAdapter(Context context, ArrayList<String> listDataHeader, HashMap<String, ArrayList<String>> listDataChild) {
        this.context = context;
        this.listDataChild = listDataChild;
        this.listDataHeader = listDataHeader;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition)
    {
        return listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition)
    {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
            boolean isLastChild, View convertView, ViewGroup parent)
    {
        final String childText = (String) getChild(groupPosition, childPosition);
        
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item, null);
        }
        
        TextView txtListChild = (TextView) convertView.findViewById(R.id.elv_item);
        txtListChild.setText(childText);
        
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition)
    {
        return listDataChild.get(listDataHeader.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition)
    {
        return listDataHeader.get(groupPosition);
    }

    @Override
    public int getGroupCount()
    {
        return listDataHeader.size();
    }

    @Override
    public long getGroupId(int groupPosition)
    {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
            View convertView, ViewGroup parent)
    {
        String headerTitle = (String) getGroup(groupPosition);
        
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_group, null);
        }
        
        TextView lblListHeader = (TextView) convertView.findViewById(R.id.elv_header);
        lblListHeader.setText(headerTitle);
        
        return convertView;
    }

    @Override
    public boolean hasStableIds()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition)
    {
        return true;
    }

}
