package com.ardeapps.livelocation.objects;

import java.util.ArrayList;

/**
 * Created by Arttu on 8.9.2017.
 */

public class ShareResource {
    public ArrayList<String> friendIdsToShare;
    public Long endTime;
    public LocationShare.ShareType shareType;

    public ShareResource clone() {
        ShareResource clone = new ShareResource();
        clone.friendIdsToShare = this.friendIdsToShare;
        clone.endTime = this.endTime;
        clone.shareType = this.shareType;
        return clone;
    }
}
