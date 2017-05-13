package org.particleframework.tck.accessories

import org.particleframework.tck.FuelTank
import org.particleframework.tck.Tire

import javax.inject.Inject

class SpareTire extends Tire {

    FuelTank constructorInjection = NEVER_INJECTED
    @Inject FuelTank fieldInjection = NEVER_INJECTED
    FuelTank methodInjection = NEVER_INJECTED
    @Inject static FuelTank staticFieldInjection = NEVER_INJECTED
    static FuelTank staticMethodInjection = NEVER_INJECTED

    @Inject
    SpareTire(FuelTank forSupertype, FuelTank forSubtype) {
        super(forSupertype)
        this.constructorInjection = forSubtype
    }

    @Inject void subtypeMethodInjection(FuelTank methodInjection) {
        if (!hasSpareTireBeenFieldInjected()) {
            methodInjectedBeforeFields = true
        }
        this.methodInjection = methodInjection
    }

    @Inject static void subtypeStaticMethodInjection(FuelTank methodInjection) {
        if (!hasBeenStaticFieldInjected()) {
            staticMethodInjectedBeforeStaticFields = true
        }
        staticMethodInjection = methodInjection
    }

    @Inject private void injectPrivateMethod() {
        if (subPrivateMethodInjected) {
            similarPrivateMethodInjectedTwice = true
        }
        subPrivateMethodInjected = true
    }

    @Inject void injectPackagePrivateMethod() {
        if (subPackagePrivateMethodInjected) {
            similarPackagePrivateMethodInjectedTwice = true
        }
        subPackagePrivateMethodInjected = true
    }

    @Inject protected void injectProtectedMethod() {
        if (subProtectedMethodInjected) {
            overriddenProtectedMethodInjectedTwice = true
        }
        subProtectedMethodInjected = true
    }

    @Inject
    void injectPublicMethod() {
        if (subPublicMethodInjected) {
            overriddenPublicMethodInjectedTwice = true
        }
        subPublicMethodInjected = true
    }

    private void injectPrivateMethodForOverride() {
        superPrivateMethodForOverrideInjected = true
    }

    void injectPackagePrivateMethodForOverride() {
        superPackagePrivateMethodForOverrideInjected = true
    }

    protected void injectProtectedMethodForOverride() {
        protectedMethodForOverrideInjected = true
    }

    void injectPublicMethodForOverride() {
        publicMethodForOverrideInjected = true
    }

    boolean hasSpareTireBeenFieldInjected() {
        return fieldInjection != NEVER_INJECTED
    }

    boolean hasSpareTireBeenMethodInjected() {
        return methodInjection != NEVER_INJECTED
    }

    static boolean hasBeenStaticFieldInjected() {
        return staticFieldInjection != NEVER_INJECTED
    }

    static boolean hasBeenStaticMethodInjected() {
        return staticMethodInjection != NEVER_INJECTED
    }

    public boolean packagePrivateMethod2Injected

    @Inject void injectPackagePrivateMethod2() {
        packagePrivateMethod2Injected = true
    }

    public boolean packagePrivateMethod3Injected

    void injectPackagePrivateMethod3() {
        packagePrivateMethod3Injected = true
    }
}