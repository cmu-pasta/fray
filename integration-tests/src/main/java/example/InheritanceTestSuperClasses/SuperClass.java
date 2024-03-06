package example.InheritanceTestSuperClasses;

public class SuperClass extends SuperSuperClass {

    public volatile Integer sc_volatile_pub;
    protected volatile Integer sc_volatile_pro;
    public Integer sc_nonvolatile_pub;

    // SuperSuperClass also has a field with the same name but is private and non-volatile
    public volatile Integer ssc_pri;

    // SuperSuperClass also has a field with the same name but is protected and volatile
    public Integer ssc_pro;

    // SuperSuperClass also has a field with the same name and same modifiers
    public volatile Integer volatile_pub;

    public void sc_method(int input) {
        ssc_volatile_pro = input;
        super.volatile_pub = input;

        // accessing shadowed fields
        ssc_pro = input;       // non-volatile
        super.ssc_pro = input; // volatile
    }
}
