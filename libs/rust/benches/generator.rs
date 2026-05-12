use afidgen::{Generator, LONG_LEN, random_long, random_short};
use criterion::{Criterion, criterion_group, criterion_main};
use std::hint::black_box;
use uuid::Uuid;

fn bench_generate(c: &mut Criterion) {
    let mut g = Generator::short("ent").unwrap();
    c.bench_function("Generator::short generate", |b| {
        b.iter(|| black_box(g.generate()));
    });

    let mut g = Generator::long("ent").unwrap();
    c.bench_function("Generator::long generate", |b| {
        b.iter(|| black_box(g.generate()));
    });
}

fn bench_generate_into(c: &mut Criterion) {
    let mut g = Generator::short("ent").unwrap();
    let mut buf = [0u8; LONG_LEN];
    c.bench_function("Generator::short generate_into", |b| {
        b.iter(|| {
            let id = g.generate_into(&mut buf);
            black_box(id);
        });
    });

    let mut g = Generator::long("ent").unwrap();
    c.bench_function("Generator::long generate_into", |b| {
        b.iter(|| {
            let id = g.generate_into(&mut buf);
            black_box(id);
        });
    });
}

fn bench_one_shot(c: &mut Criterion) {
    c.bench_function("random_short", |b| {
        b.iter(|| black_box(random_short(black_box("ent")).unwrap()));
    });
    c.bench_function("random_long", |b| {
        b.iter(|| black_box(random_long(black_box("ent")).unwrap()));
    });
}

// Baseline: `uuid` crate v4 (the de facto Rust UUID library).
fn bench_uuid_v4(c: &mut Criterion) {
    c.bench_function("uuid::Uuid::new_v4 to_string", |b| {
        b.iter(|| black_box(Uuid::new_v4().to_string()));
    });
}

criterion_group!(
    benches,
    bench_generate,
    bench_generate_into,
    bench_one_shot,
    bench_uuid_v4
);
criterion_main!(benches);
