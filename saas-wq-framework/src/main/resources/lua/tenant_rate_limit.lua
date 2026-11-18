-- Fixed window counter executed atomically inside Redis.
-- KEYS[1] counter key of the tenant for the current window
-- ARGV[1] counter lifetime in seconds
-- Returns the request count after the increment.
local current = redis.call('INCR', KEYS[1])
if current == 1 then
    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
end
return current
