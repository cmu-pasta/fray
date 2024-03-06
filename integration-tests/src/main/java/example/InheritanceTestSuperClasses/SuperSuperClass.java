package example.InheritanceTestSuperClasses;

public class SuperSuperClass {
    
    public volatile Integer ssc_volatile_pub;
    protected volatile Integer ssc_volatile_pro = 0;

    // SuperClass also has a field with the same name but is public and volatile
    private Integer ssc_pri;

    // SuperClass also has a field with the same name but is public and non-volatile
    protected volatile Integer ssc_pro;

    // SuperClass also has a field with the same name and same modifiers
    public volatile Integer volatile_pub;
}
