// script must return a single closure
return { name ->
    println "${Thread.currentThread().name} -> sup $name"
    return 12345
}