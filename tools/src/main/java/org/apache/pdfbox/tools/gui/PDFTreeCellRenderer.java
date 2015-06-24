/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pdfbox.tools.gui;

import java.awt.Component;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.tools.pdfdebugger.ui.OverlayIcon;

/**
 * A class to render tree cells for the pdfviewer.
 *
 * @author Ben Litchfield
 */
public class PDFTreeCellRenderer extends DefaultTreeCellRenderer
{
    private final ImageIcon ICON_ARRAY = new ImageIcon(getImageUrl("array"));
    private final ImageIcon ICON_DICT = new ImageIcon(getImageUrl("dict"));
    private final ImageIcon ICON_HEX = new ImageIcon(getImageUrl("hex"));
    private final ImageIcon ICON_INDIRECT = new ImageIcon(getImageUrl("indirect"));
    private final ImageIcon ICON_INTEGER = new ImageIcon(getImageUrl("integer"));
    private final ImageIcon ICON_NAME = new ImageIcon(getImageUrl("name"));
    //private final ImageIcon ICON_NULL = new ImageIcon(getImageUrl("null"));
    private final ImageIcon ICON_REAL = new ImageIcon(getImageUrl("real"));
    private final ImageIcon ICON_STREAM_DICT = new ImageIcon(getImageUrl("stream-dict"));
    private final ImageIcon ICON_STRING = new ImageIcon(getImageUrl("string"));

    private static URL getImageUrl(String name)
    {
        String fullName = "/org/apache/pdfbox/tools/pdfdebugger/" + name + ".png";
        return PDFTreeCellRenderer.class.getResource(fullName);
    }
    
    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object nodeValue,
            boolean isSelected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean componentHasFocus)
    {
        Component component = super.getTreeCellRendererComponent(tree,
                toTreeObject(nodeValue),
                isSelected, expanded, leaf, row, componentHasFocus);
        
        setIcon(lookupIconWithOverlay(nodeValue));

        return component;
    }

    private Object toTreeObject(Object nodeValue)
    {
        Object result = nodeValue;
        if (nodeValue instanceof MapEntry || nodeValue instanceof ArrayEntry)
        {
            String key;
            Object value;
            COSBase item;
            if (nodeValue instanceof MapEntry)
            {
                MapEntry entry = (MapEntry) nodeValue;
                key = entry.getKey().getName();
                value = toTreeObject(entry.getValue());
                item = entry.getItem();
            }
            else
            {
                ArrayEntry entry = (ArrayEntry) nodeValue;
                key = "" + entry.getIndex();
                value = toTreeObject(entry.getValue());
                item = entry.getItem();
            }
            
            String stringResult = key;
            if (value instanceof String && ((String)value).length() > 0)
            {
                stringResult += ":  " + value;
                if (item instanceof COSObject)
                {
                    COSObject indirect = (COSObject)item;
                    stringResult += " [" + indirect.getObjectNumber() + " " +
                                           indirect.getGenerationNumber() + " R]";
                }
                
            }
            result = stringResult;
        }
        else if (nodeValue instanceof COSFloat)
        {
            result = "" + ((COSFloat) nodeValue).floatValue();
        }
        else if (nodeValue instanceof COSInteger)
        {
            result = "" + ((COSInteger) nodeValue).intValue();
        }
        else if (nodeValue instanceof COSString)
        {
            String text = ((COSString) nodeValue).getString();
            // display unprintable strings as hex
            for (char c : text.toCharArray())
            {
                if (Character.isISOControl(c))
                {
                    text = "<" + ((COSString) nodeValue).toHexString() + ">";
                    break;
                }
            }
            result = text;
        }
        else if (nodeValue instanceof COSName)
        {
            result = ((COSName) nodeValue).getName();
        }
        else if (nodeValue instanceof COSNull)
        {
            result = "";
        }
        else if (nodeValue instanceof COSDictionary)
        {
            COSDictionary dict = (COSDictionary) nodeValue;
            if (COSName.XREF.equals(dict.getCOSName(COSName.TYPE)))
            {
                result = "";
            }
            else
            {
                result = "(" + dict.size() + ")";
            }
        }
        else if (nodeValue instanceof COSArray)
        {
            COSArray array = (COSArray) nodeValue;
            result = "(" + array.size() + ")";
        }
        return result;
    }

    private ImageIcon lookupIconWithOverlay(Object nodeValue)
    {
        ImageIcon icon = lookupIcon(nodeValue);
        boolean isIndirect = false;
        boolean isStream = false;
        
        if (nodeValue instanceof MapEntry)
        {
            MapEntry entry = (MapEntry) nodeValue;
            if (entry.getItem() instanceof COSObject)
            {
                isIndirect = true;
                isStream = entry.getValue() instanceof COSStream;
            }
        }
        else if (nodeValue instanceof ArrayEntry)
        {
            ArrayEntry entry = (ArrayEntry) nodeValue;
            if (entry.getItem() instanceof COSObject)
            {
                isIndirect = true;
                isStream = entry.getValue() instanceof COSStream;
            }
        }
        
        if (isIndirect && !isStream)
        {
            OverlayIcon overlay = new OverlayIcon(icon);
            overlay.add(ICON_INDIRECT);
            return overlay;
        }
        return icon;
    }
    
    private ImageIcon lookupIcon(Object nodeValue)
    {
        if (nodeValue instanceof MapEntry)
        {
            MapEntry entry = (MapEntry) nodeValue;
            return lookupIcon(entry.getValue());
        }
        else if (nodeValue instanceof ArrayEntry)
        {
            ArrayEntry entry = (ArrayEntry) nodeValue;
            return lookupIcon(entry.getValue());
        }
        else if (nodeValue instanceof COSFloat)
        {
            return ICON_REAL;
        }
        else if (nodeValue instanceof COSInteger)
        {
            return ICON_INTEGER;
        }
        else if (nodeValue instanceof COSString)
        {
            String text = ((COSString) nodeValue).getString();
            // display unprintable strings as hex
            for (char c : text.toCharArray())
            {
                if (Character.isISOControl(c))
                {
                    return ICON_HEX;
                }
            }
            return ICON_STRING;
        }
        else if (nodeValue instanceof COSName)
        {
            return ICON_NAME;
        }
        else if (nodeValue instanceof COSNull)
        {
            return null; //ICON_NULL;
        }
        else if (nodeValue instanceof COSStream)
        {
            return ICON_STREAM_DICT;
        }
        else if (nodeValue instanceof COSDictionary)
        {
            return ICON_DICT;
        }
        else if (nodeValue instanceof COSArray)
        {
            return ICON_ARRAY;
        }
        else
        {
            return null;
        }
    }
}
