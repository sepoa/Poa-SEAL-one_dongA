/**
 * dhtmlxLayout
 */
function SepoaDhtmlXToolbarObject(c, e){
    var base = dhtmlXToolbarObject.call(this, c, e);
}
SepoaDhtmlXToolbarObject.prototype = Object.create(dhtmlXToolbarObject.prototype);
SepoaDhtmlXToolbarObject.prototype.constructor = SepoaDhtmlXToolbarObject;
SepoaDhtmlXToolbarObject.prototype.getObject = function(e) {
    var g = this.getParentId(e);
    if (g != null) {
        var c = null;
        var a = this.objPull[this.idPrefix + g]._listOptions[e];
        if (a != null) {
            if (a.sep != null) {
                c = "buttonSelectSeparator"
            } else {
                c = "buttonSelectButton"
            }
        }
        return c
    } else {
        if (this.objPull[this.idPrefix + e] == null) {
            return null
        }
        return this.objPull[this.idPrefix + e]
    }
};