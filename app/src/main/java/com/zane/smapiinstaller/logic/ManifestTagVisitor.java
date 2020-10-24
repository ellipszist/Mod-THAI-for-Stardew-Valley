package com.zane.smapiinstaller.logic;

import java.util.List;
import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import pxb.android.axml.NodeVisitor;

/**
 * AndroidManifest文件节点访问器
 */
class ManifestTagVisitor extends NodeVisitor {

    private final Function<AttrArgs, List<AttrArgs>> attrProcessLogic;
    private final Function<ChildArgs, List<ChildArgs>> childProcessLogic;

    public ManifestTagVisitor(NodeVisitor nv, Function<AttrArgs, List<AttrArgs>> attrProcessLogic, Function<ChildArgs, List<ChildArgs>> childProcessLogic) {
        super(nv);
        this.attrProcessLogic = attrProcessLogic;
        this.childProcessLogic = childProcessLogic;
    }

    @Override
    public void attr(String ns, String name, int resourceId, int type, Object obj) {
        AttrArgs attrArgs = new AttrArgs(ns, name, resourceId, type, obj);
        List<AttrArgs> appendAttrs = attrProcessLogic.apply(attrArgs);
        super.attr(attrArgs.ns, attrArgs.name, attrArgs.resourceId, attrArgs.type, attrArgs.obj);
        if(appendAttrs != null) {
            for (AttrArgs attr: appendAttrs) {
                super.attr(attr.ns, attr.name, attr.resourceId, attr.type, attr.obj);
            }
        }
    }

    @Override
    public NodeVisitor child(String ns, String name) {
        ChildArgs childArgs = new ChildArgs(ns, name, null);
        List<ChildArgs> appendChild = childProcessLogic.apply(childArgs);
        NodeVisitor child = super.child(childArgs.ns, childArgs.name);
        if(appendChild != null) {
            for (ChildArgs c: appendChild) {
                NodeVisitor visitor = super.child(c.ns, c.name);
                if(c.attrArgs != null) {
                    for (AttrArgs attr: c.attrArgs) {
                        visitor.attr(attr.ns, attr.name, attr.resourceId, attr.type, attr.obj);
                    }
                }
            }
        }
        return new ManifestTagVisitor(child, attrProcessLogic, childProcessLogic);
    }
    @AllArgsConstructor
    public static class ChildArgs {
        String ns;
        String name;
        List<AttrArgs> attrArgs;
    }
    @AllArgsConstructor
    public static class AttrArgs {
        String ns;
        String name;
        int resourceId;
        int type;
        Object obj;
    }
}
