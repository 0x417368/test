package im.conversations.android.xmpp.model.reply;

import eu.siacs.conversations.xml.Namespace;
import eu.siacs.conversations.xmpp.Jid;
import im.conversations.android.annotation.XmlElement;
import im.conversations.android.xmpp.model.Extension;

@XmlElement(namespace = Namespace.REPLY)
public class Reply extends Extension {

    public Reply() {
        super(Reply.class);
    }

    public Jid getTo() {
        return this.getAttributeAsJid("to");
    }

    public String getId() {
        return this.getAttribute("id");
    }

    public void setTo(final Jid to) {
        this.setAttribute("to", to);
    }

    public void setId(final String id) {
        this.setAttribute("id", id);
    }
}
