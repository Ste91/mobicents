/*
 * CallSbb.java
 *
 * The source code contained in this file is in in the public domain.
 * It can be used in any project or product without prior permission,
 * license or royalty payments. There is  NO WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR STATUTORY, INCLUDING, WITHOUT LIMITATION,
 * THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * AND DATA ACCURACY.  We do not warrant or make any representations
 * regarding the use of the software or the  results thereof, including
 * but not limited to the correctness, accuracy, reliability or
 * usefulness of the software.
 */

package org.mobicents.examples.media.events;

import java.io.Serializable;
import org.mobicents.mscontrol.MsLink;

/**
 *
 * @author Oleg Kulikov
 */
public class DialogStartedEvent implements Serializable {
    private MsLink link;
    
    public DialogStartedEvent(MsLink link) {
        this.link = link;
    }
    
    public MsLink getLink() {
        return link;
    }
    
    @Override
    public boolean equals(Object other) {
        return (other instanceof DialogCompletedEvent) &&
                ((DialogStartedEvent) other).link.equals(link);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 61 * hash + (this.link != null ? this.link.hashCode() : 0);
        return hash;
    }
    
}
