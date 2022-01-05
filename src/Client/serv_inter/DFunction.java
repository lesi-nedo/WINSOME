package serv_inter;

@FunctionalInterface
interface DFunction<A,B> {

    void apply(A a, B b);

    
}