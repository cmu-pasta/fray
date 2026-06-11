using System.Text.Json;
using System.Text.Json.Serialization;

namespace Fray.Core.Randomness;

/// <summary>Source of scheduling randomness; recordable for deterministic replay.</summary>
[JsonPolymorphic(TypeDiscriminatorPropertyName = "type")]
[JsonDerivedType(typeof(ControlledRandom), "controlled")]
public interface IRandomness
{
    /// <summary>A non-negative pseudo-random integer.</summary>
    int NextInt();

    double NextDouble();

    double NextDouble(double origin, double bound);
}

/// <summary>
/// Records every random value it produces so an execution can be replayed
/// exactly. When pre-populated (from a recording), values are returned in
/// order before any new ones are generated.
///
/// Mirrors <c>org.pastalab.fray.core.randomness.ControlledRandom</c>.
/// </summary>
public sealed class ControlledRandom : IRandomness
{
    private static readonly JsonSerializerOptions JsonOptions = new() { WriteIndented = true };

    [JsonIgnore] private readonly Random _random;
    [JsonIgnore] private int _integerIndex;
    [JsonIgnore] private int _doubleIndex;

    public List<int> Integers { get; init; } = new();
    public List<double> Doubles { get; init; } = new();

    public ControlledRandom() : this(new Random()) { }

    public ControlledRandom(Random random) => _random = random;

    public ControlledRandom(int seed) : this(new Random(seed)) { }

    public int NextInt()
    {
        if (_integerIndex >= Integers.Count)
        {
            var value = _random.Next(int.MaxValue);
            Integers.Add(value);
            _integerIndex += 1;
            return value;
        }
        return Integers[_integerIndex++];
    }

    public double NextDouble()
    {
        if (_doubleIndex >= Doubles.Count)
        {
            var value = _random.NextDouble();
            Doubles.Add(value);
            _doubleIndex += 1;
            return value;
        }
        return Doubles[_doubleIndex++];
    }

    public double NextDouble(double origin, double bound)
    {
        if (_doubleIndex >= Doubles.Count)
        {
            var value = origin + _random.NextDouble() * (bound - origin);
            Doubles.Add(value);
            _doubleIndex += 1;
            return value;
        }
        return Doubles[_doubleIndex++];
    }

    /// <summary>Snapshot of all values produced so far, for saving to a report.</summary>
    public ControlledRandom Snapshot() => new(new Random())
    {
        Integers = new List<int>(Integers),
        Doubles = new List<double>(Doubles),
    };

    public string ToJson() => JsonSerializer.Serialize(this, JsonOptions);

    public static ControlledRandom FromJson(string json) =>
        JsonSerializer.Deserialize<ControlledRandom>(json)
        ?? throw new FrayInternalException("Failed to deserialize ControlledRandom.");
}
