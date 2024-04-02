package com.huanli233.apkpatcher.dexlib2;

import java.util.Set;

import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodParameter;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.HiddenApiRestriction;
import com.android.tools.smali.dexlib2.iface.Annotation;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;

public class MethodWrapper {
    private String definingClass;
    private String name;
    private Iterable<? extends MethodParameter> parameters;
    private String returnType;
    private int accessFlags;
    private Set<? extends Annotation> annotations;
    private Set<HiddenApiRestriction> hiddenApiRestrictions;
    private MethodImplementation methodImplementation;

    public MethodWrapper(Method method) {
        this.definingClass = method.getDefiningClass();
        this.name = method.getName();
        this.parameters = method.getParameters();
        this.returnType = method.getReturnType();
        this.accessFlags = method.getAccessFlags();
        this.annotations = method.getAnnotations();
        this.hiddenApiRestrictions = method.getHiddenApiRestrictions();
        this.methodImplementation = method.getImplementation();
    }
    
    public MethodWrapper apply(MethodImplementation methodImplementation) {
		this.methodImplementation = methodImplementation;
		return this;
	}

    public ImmutableMethod build() {
        return new ImmutableMethod(
            definingClass,
            name,
            parameters,
            returnType,
            accessFlags,
            annotations,
            hiddenApiRestrictions,
            methodImplementation
        );
    }
}
