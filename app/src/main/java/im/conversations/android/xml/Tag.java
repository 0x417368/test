/*
 * Copyright (c) 2023, Daniel Gultsch
 *
 * This file is part of Conversations.
 *
 * Conversations is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Conversations is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Conversations.  If not, see <https://www.gnu.org/licenses/>.
 */

package im.conversations.android.xml;

import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jxmpp.jid.Jid;

public class Tag {
    public static final int NO = -1;
    public static final int START = 0;
    public static final int END = 1;
    public static final int EMPTY = 2;

    protected int type;
    protected String name;
    protected Hashtable<String, String> attributes = new Hashtable<String, String>();

    protected Tag(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public static Tag no(String text) {
        return new Tag(NO, text);
    }

    public static Tag start(String name) {
        return new Tag(START, name);
    }

    public static Tag end(String name) {
        return new Tag(END, name);
    }

    public static Tag empty(String name) {
        return new Tag(EMPTY, name);
    }

    public String getName() {
        return name;
    }

    public String getAttribute(final String attrName) {
        return this.attributes.get(attrName);
    }

    public Tag setAttribute(final String attrName, final String attrValue) {
        this.attributes.put(attrName, attrValue);
        return this;
    }

    public Tag setAttribute(final String attrName, final Jid attrValue) {
        if (attrValue != null) {
            this.attributes.put(attrName, attrValue.toString());
        }
        return this;
    }

    public void setAttributes(final Hashtable<String, String> attributes) {
        this.attributes = attributes;
    }

    public boolean isStart(final String needle) {
        if (needle == null) {
            return false;
        }
        return (this.type == START) && (needle.equals(this.name));
    }

    public boolean isStart(final String name, final String namespace) {
        return isStart(name) && namespace != null && namespace.equals(this.getAttribute("xmlns"));
    }

    public boolean isEnd(String needle) {
        if (needle == null) return false;
        return (this.type == END) && (needle.equals(this.name));
    }

    public boolean isNo() {
        return (this.type == NO);
    }

    @NotNull
    public String toString() {
        final StringBuilder tagOutput = new StringBuilder();
        tagOutput.append('<');
        if (type == END) {
            tagOutput.append('/');
        }
        tagOutput.append(name);
        if (type != END) {
            final Set<Entry<String, String>> attributeSet = attributes.entrySet();
            for (final Entry<String, String> entry : attributeSet) {
                tagOutput.append(' ');
                tagOutput.append(entry.getKey());
                tagOutput.append("=\"");
                tagOutput.append(Entities.encode(entry.getValue()));
                tagOutput.append('"');
            }
        }
        if (type == EMPTY) {
            tagOutput.append('/');
        }
        tagOutput.append('>');
        return tagOutput.toString();
    }

    public Hashtable<String, String> getAttributes() {
        return this.attributes;
    }
}
