if redis.call('SET', KEYS[1], '1', 'NX', 'PX', ARGV[1]) then
    redis.call('INCR', KEYS[2])
    redis.call('SADD', KEYS[3], ARGV[2])
    return 1
end
return 0
